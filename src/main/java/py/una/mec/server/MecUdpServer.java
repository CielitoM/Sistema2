package py.una.mec.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import py.una.comun.json.MensajeOperacionJSON;
import py.una.comun.protocolo.MensajeOperacion;
import py.una.mec.dao.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import py.una.mec.util.LogUtil;

public class MecUdpServer implements Runnable {
    private static final java.util.logging.Logger LOG = LogUtil.get(MecUdpServer.class);
    public static final int PUERTO = 9876;
    private volatile boolean escuchando = true;
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(PUERTO);
            LOG.info("MEC UDP escuchando en el puerto " + PUERTO);
            while (escuchando) {
                byte[] buffer = new byte[1024];
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);
                // Decodifica solo el contenido del datagrama, sin los bytes sobrantes del buffer.
                String texto = new String(paquete.getData(), paquete.getOffset(), paquete.getLength(), "UTF-8");
                MensajeOperacion solicitud = MensajeOperacionJSON.deserializar(texto);
                MensajeOperacion respuesta = procesarIdempotente(solicitud);
                byte[] datos = MensajeOperacionJSON.serializar(respuesta).getBytes("UTF-8");
                // Responde al origen del datagrama: el cliente usa su propio puerto,
                // que puede ser distinto del puerto de escucha del servidor.
                DatagramPacket respuestaPaquete = new DatagramPacket(
                    datos, datos.length, paquete.getAddress(), paquete.getPort());
                socket.send(respuestaPaquete);
            }
        } catch (Exception e) {
            // El cierre solicitado en detener() puede interrumpir receive().
            if (escuchando) {
                LOG.warning("Error en servidor UDP MEC: " + e.getMessage());
            }
        } finally {
            if (socket != null) socket.close();
        }
    }

    private synchronized MensajeOperacion procesarIdempotente(MensajeOperacion solicitud) {
        try (Connection c = Conexion.abrir();
             PreparedStatement p = c.prepareStatement("SELECT respuesta FROM udp_idempotencia WHERE id_solicitud=?")) {
            p.setString(1, solicitud.getIdSolicitud());
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return MensajeOperacionJSON.deserializar(r.getString(1));
            }
        } catch (Exception e) {
            return error(solicitud, "No se pudo consultar idempotencia: " + e.getMessage());
        }
        MensajeOperacion respuesta = validarTitulo(solicitud);
        try (Connection c = Conexion.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO udp_idempotencia(id_solicitud,solicitud,respuesta) VALUES(?,?,?)");
                PreparedStatement a = c.prepareStatement(
                "INSERT INTO auditoria(origen,tipo_operacion,id_solicitud,resultado,detalle) VALUES(?,?,?,?,?)")) {
                p.setString(1, solicitud.getIdSolicitud());
                p.setString(2, MensajeOperacionJSON.serializar(solicitud));
                p.setString(3, MensajeOperacionJSON.serializar(respuesta));
                p.executeUpdate();
                a.setString(1, "FPUNA"); a.setString(2, solicitud.getTipoOperacion());
                a.setString(3, solicitud.getIdSolicitud()); a.setString(4, respuesta.getEstado());
                a.setString(5, respuesta.getMensaje()); a.executeUpdate();
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            return error(solicitud, "No se pudo guardar idempotencia: " + e.getMessage());
        }
        return respuesta;
    }

    private MensajeOperacion validarTitulo(MensajeOperacion solicitud) {
                try {
                    String[] d = solicitud.getDatos().split("\\|", -1);
                    if (d.length < 2) throw new IllegalArgumentException("Se requiere CI y carrera");
                    try (Connection c = Conexion.abrir();
                         PreparedStatement p = c.prepareStatement(
                             "SELECT numero,universidad FROM titulo WHERE ci=? AND carrera=? AND valido=TRUE")) {
                        p.setString(1, d[0]); p.setString(2, d[1]);
                        try (ResultSet r = p.executeQuery()) {
                            if (r.next()) return new MensajeOperacion("RESPUESTA_VALIDACION_TITULO",
                                solicitud.getIdSolicitud(), "OK", "Título válido", r.getString(1)+"|"+r.getString(2));
                        }
                    }
                    return new MensajeOperacion("RESPUESTA_VALIDACION_TITULO", solicitud.getIdSolicitud(),
                        "OK", "Título no válido o no encontrado", "");
                } catch (Exception e) {
                    return new MensajeOperacion("RESPUESTA_VALIDACION_TITULO", solicitud.getIdSolicitud(),
                        "ERROR", "No se pudo validar el título: " + e.getMessage(), "");
                }
        }
    private MensajeOperacion error(MensajeOperacion s, String mensaje) {
        return new MensajeOperacion("RESPUESTA_" + s.getTipoOperacion(), s.getIdSolicitud(),
            "ERROR", mensaje, "");
    }

    public void detener() {
        escuchando = false;
        if (socket != null) socket.close();
    }
}
