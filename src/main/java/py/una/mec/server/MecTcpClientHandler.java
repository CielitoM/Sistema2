package py.una.mec.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import py.una.comun.json.MensajeOperacionJSON;
import py.una.comun.protocolo.MensajeOperacion;
import py.una.mec.dao.Conexion;
import py.una.mec.util.LogUtil;

public class MecTcpClientHandler extends Thread {
    private static final java.util.logging.Logger LOG = LogUtil.get(MecTcpClientHandler.class);
    private final Socket socket;
    private final MecTcpServer servidor;

    public MecTcpClientHandler(Socket socket, MecTcpServer servidor) {
        super("MecTcpClientHandler");
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override public void run() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {
            salida.println(MensajeOperacionJSON.serializar(new MensajeOperacion("BIENVENIDA", "-", "Conectado al MEC")));
            String linea;
            while ((linea = entrada.readLine()) != null) {
                MensajeOperacion solicitud = MensajeOperacionJSON.deserializar(linea);
                salida.println(MensajeOperacionJSON.serializar(procesar(solicitud)));
            }
        } catch (Exception e) {
            LOG.warning("Conexión TCP MEC finalizada: " + e.getMessage());
        } finally {
            servidor.eliminar(this);
            try { socket.close(); } catch (IOException ignorado) { }
        }
    }

    private MensajeOperacion procesar(MensajeOperacion s) {
        try {
            String t = s.getTipoOperacion();
            if ("RECEPCION_DATOS_ACADEMICOS".equals(t)) return reporte(s);
            if ("CONSULTA_EGRESADOS".equals(t)) return egresados(s);
            if ("CONSULTA_MALLA".equals(t)) return malla(s);
            if ("CERTIFICADO_NACIONAL".equals(t)) return certificado(s);
            if ("AUDITORIA_CONSULTA".equals(t)) return auditoria(s);
            return respuesta(s, "ERROR", "Operación TCP desconocida: " + t, "");
        } catch (Exception e) {
            return respuesta(s, "ERROR", e.getMessage(), "");
        }
    }

    private MensajeOperacion reporte(MensajeOperacion s) throws Exception {
        String[] d = partes(s.getDatos(), 7);
        try (Connection c = Conexion.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO estudiante_reportado(ci,nombres,apellidos,carrera,universidad,estado,promedio) VALUES(?,?,?,?,?,?,?) "
                    + "ON CONFLICT(ci) DO UPDATE SET nombres=EXCLUDED.nombres,apellidos=EXCLUDED.apellidos,"
                    + "carrera=EXCLUDED.carrera,universidad=EXCLUDED.universidad,estado=EXCLUDED.estado,"
                    + "promedio=EXCLUDED.promedio,actualizado_en=CURRENT_TIMESTAMP")) {
                for (int i = 0; i < 6; i++) p.setString(i + 1, d[i]);
                p.setDouble(7, Double.parseDouble(d[6]));
                p.executeUpdate();
            }
            auditar(c, s, "OK", d[4], "Reporte académico guardado");
            c.commit();
        } catch (Exception e) {
            try (Connection c = Conexion.abrir()) {
                auditar(c, s, "ERROR", d[4], "Reporte rechazado: " + e.getMessage());
            } catch (Exception ignorado) { }
            throw e;
        }
        return respuesta(s, "OK", "Reporte académico guardado", "");
    }

    private MensajeOperacion egresados(MensajeOperacion s) throws Exception {
        String filtro = s.getDatos() == null ? "" : s.getDatos();
        StringBuilder out = new StringBuilder();
        try (Connection c = Conexion.abrir();
             PreparedStatement p = c.prepareStatement(
                 "SELECT ci,nombres,apellidos,carrera,universidad,promedio FROM estudiante_reportado "
                 + "WHERE estado='egresado' AND (?='' OR ci=? OR carrera ILIKE ? OR universidad ILIKE ?)")) {
            p.setString(1, filtro); p.setString(2, filtro);
            p.setString(3, "%" + filtro + "%"); p.setString(4, "%" + filtro + "%");
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.append(r.getString(1)).append(" | ").append(r.getString(2))
                    .append(' ').append(r.getString(3)).append(" | ").append(r.getString(4))
                    .append(" | ").append(r.getString(5)).append(" | ").append(r.getString(6)).append('\n');
            }
        }
        auditar(s, "OK", "", "Consulta de egresados");
        return respuesta(s, "OK", "Consulta de egresados", out.toString());
    }

    private MensajeOperacion malla(MensajeOperacion s) throws Exception {
        StringBuilder out = new StringBuilder();
        try (Connection c = Conexion.abrir();
             PreparedStatement p = c.prepareStatement(
                 "SELECT m.nombre,m.semestre,m.creditos,m.carga_horaria FROM materia_malla m "
                 + "JOIN carrera ca ON ca.id=m.carrera_id WHERE ca.nombre=? ORDER BY m.semestre,m.nombre")) {
            p.setString(1, s.getDatos());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.append("Semestre ").append(r.getInt(2)).append(": ")
                    .append(r.getString(1)).append(" | créditos=").append(r.getString(3))
                    .append(" | horas=").append(r.getString(4)).append('\n');
            }
        }
        auditar(s, "OK", "", "Consulta de malla");
        return respuesta(s, "OK", "Malla curricular consultada", out.toString());
    }

    private MensajeOperacion certificado(MensajeOperacion s) throws Exception {
        String[] d = s.getDatos() == null ? new String[0] : s.getDatos().split("\\|", -1);
        if (d.length == 3 && "EMITIR".equals(d[0])) {
            String[] datos = new String[] { d[1], d[2] };
            try (Connection c = Conexion.abrir()) {
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT numero FROM titulo WHERE ci=? AND carrera=? AND valido=TRUE")) {
                    q.setString(1, datos[0]); q.setString(2, datos[1]);
                    try (ResultSet r = q.executeQuery()) {
                        if (!r.next()) return respuesta(s, "OK", "Título no válido o no encontrado", "");
                    }
                }
                String numero = "CERT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO certificado_nacional(numero,ci,carrera) VALUES(?,?,?)")) {
                    p.setString(1, numero); p.setString(2, datos[0]); p.setString(3, datos[1]);
                    p.executeUpdate();
                }
                auditar(s, "OK", "", "Certificado nacional emitido");
                return respuesta(s, "OK", "Certificado nacional emitido", numero + "|" + datos[0] + "|" + datos[1]);
            }
        }
        if (d.length != 2) throw new IllegalArgumentException("CERTIFICADO_NACIONAL requiere operación y valor");
        String sql = "CONSULTAR_NUMERO".equals(d[0])
            ? "SELECT numero,ci,carrera,emitido_en FROM certificado_nacional WHERE numero=?"
            : "SELECT numero,ci,carrera,emitido_en FROM certificado_nacional WHERE ci=?";
        try (Connection c = Conexion.abrir(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, d[1]);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return respuesta(s, "OK", "Certificado encontrado",
                    r.getString(1) + "|" + r.getString(2) + "|" + r.getString(3) + "|" + r.getString(4));
            }
        }
        return respuesta(s, "OK", "Certificado no encontrado", "");
    }

    private MensajeOperacion auditoria(MensajeOperacion s) throws Exception {
        StringBuilder out = new StringBuilder();
        try (Connection c = Conexion.abrir();
             PreparedStatement p = c.prepareStatement(
                 "SELECT fecha,origen,tipo_operacion,id_solicitud,resultado,universidad,detalle "
                 + "FROM auditoria ORDER BY fecha DESC LIMIT 200");
             ResultSet r = p.executeQuery()) {
            while (r.next()) out.append(r.getTimestamp(1)).append(" | ").append(r.getString(2))
                .append(" | ").append(r.getString(3)).append(" | ").append(r.getString(4))
                .append(" | ").append(r.getString(5)).append(" | ").append(r.getString(6))
                .append(" | ").append(r.getString(7)).append('\n');
        }
        return respuesta(s, "OK", "Historial de auditoría", out.toString());
    }

    private void auditar(MensajeOperacion s, String resultado, String universidad, String detalle) throws Exception {
        try (Connection c = Conexion.abrir()) { auditar(c, s, resultado, universidad, detalle); }
    }
    private void auditar(Connection c, MensajeOperacion s, String resultado, String universidad, String detalle) throws Exception {
        try (PreparedStatement p = c.prepareStatement(
            "INSERT INTO auditoria(origen,tipo_operacion,id_solicitud,universidad,resultado,detalle) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, "FPUNA"); p.setString(2, s.getTipoOperacion()); p.setString(3, s.getIdSolicitud());
            p.setString(4, universidad); p.setString(5, resultado); p.setString(6, detalle); p.executeUpdate();
        }
    }
    private MensajeOperacion respuesta(MensajeOperacion s, String estado, String mensaje, String datos) {
        return new MensajeOperacion("RESPUESTA_" + s.getTipoOperacion(), s.getIdSolicitud(), estado, mensaje, datos);
    }
    private String[] partes(String datos, int cantidad) {
        String[] r = datos == null ? new String[0] : datos.split("\\|", -1);
        if (r.length != cantidad) throw new IllegalArgumentException("Formato de datos inválido");
        return r;
    }
}
