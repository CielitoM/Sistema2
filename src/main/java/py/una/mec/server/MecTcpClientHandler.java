package py.una.mec.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import py.una.comun.json.MensajeOperacionJSON;
import py.una.comun.protocolo.MensajeOperacion;

public class MecTcpClientHandler extends Thread {
    private final Socket socket;
    private final MecTcpServer servidor;

    public MecTcpClientHandler(Socket socket, MecTcpServer servidor) {
        super("MecTcpClientHandler");
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {
            // FPUNA consume este saludo inicial antes de enviar su reporte.
            salida.println(MensajeOperacionJSON.serializar(new MensajeOperacion(
                "BIENVENIDA", "-", "Conectado al MEC")));

            String linea;
            while ((linea = entrada.readLine()) != null) {
                MensajeOperacion solicitud = MensajeOperacionJSON.deserializar(linea);
                System.out.println("MEC TCP recibió " + solicitud.getTipoOperacion()
                    + " [" + solicitud.getIdSolicitud() + "]");
                String respuesta = "Reporte académico recibido";
                if ("CONSULTA_EGRESADOS".equals(solicitud.getTipoOperacion())) {
                    respuesta = "Consulta de egresados procesada";
                }
                // Conserva el identificador para relacionar la respuesta con la solicitud.
                salida.println(MensajeOperacionJSON.serializar(new MensajeOperacion(
                    "RESPUESTA_" + solicitud.getTipoOperacion(),
                    solicitud.getIdSolicitud(), respuesta)));
            }
        } catch (Exception e) {
            System.err.println("Conexión TCP MEC finalizada: " + e.getMessage());
        } finally {
            servidor.eliminar(this);
            try { socket.close(); } catch (IOException ignorado) { }
        }
    }
}
