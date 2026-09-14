package py.una.mec.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import py.una.comun.json.MensajeOperacionJSON;
import py.una.comun.protocolo.MensajeOperacion;

public class MecUdpServer implements Runnable {
    public static final int PUERTO = 9876;
    private volatile boolean escuchando = true;
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(PUERTO);
            System.out.println("MEC UDP escuchando en el puerto " + PUERTO);
            while (escuchando) {
                byte[] buffer = new byte[1024];
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);
                // Decodifica solo el contenido del datagrama, sin los bytes sobrantes del buffer.
                String texto = new String(paquete.getData(), paquete.getOffset(), paquete.getLength(), "UTF-8");
                MensajeOperacion solicitud = MensajeOperacionJSON.deserializar(texto);
                System.out.println("MEC UDP recibió " + solicitud.getTipoOperacion()
                    + " [" + solicitud.getIdSolicitud() + "]");
                MensajeOperacion respuesta = new MensajeOperacion(
                    "RESPUESTA_" + solicitud.getTipoOperacion(),
                    solicitud.getIdSolicitud(), "Título válido para consulta nacional");
                byte[] datos = MensajeOperacionJSON.serializar(respuesta).getBytes("UTF-8");
                // Responde al origen del datagrama: el cliente usa su propio puerto,
                // que puede ser distinto del puerto de escucha del servidor.
                DatagramPacket respuestaPaquete = new DatagramPacket(
                    datos, datos.length, paquete.getAddress(), paquete.getPort());
                socket.send(respuestaPaquete);
            }
        } catch (Exception e) {
            // El cierre solicitado en detener() puede interrumpir receive().
            // Solo informa el error si el servidor todavia debia estar escuchando.
            if (escuchando) {
                System.err.println("Error en servidor UDP MEC: " + e.getMessage());
            }
        } finally {
            if (socket != null) socket.close();
        }
    }

    public void detener() {
        escuchando = false;
        if (socket != null) socket.close();
    }
}
