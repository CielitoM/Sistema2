package py.una.mec.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import py.una.mec.util.LogUtil;

public class MecTcpServer implements Runnable {
    private static final java.util.logging.Logger LOG = LogUtil.get(MecTcpServer.class);
    public static final int PUERTO = 4444;
    private volatile boolean escuchando = true;
    private ServerSocket serverSocket;
    private final List<MecTcpClientHandler> clientes = new CopyOnWriteArrayList<MecTcpClientHandler>();
    private final List<String> universidades = new CopyOnWriteArrayList<String>();

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PUERTO);
            serverSocket.setSoTimeout(1000);
            LOG.info("MEC TCP escuchando en el puerto " + PUERTO);
            while (escuchando) {
                try {
                    Socket socket = serverSocket.accept();
                    MecTcpClientHandler handler = new MecTcpClientHandler(socket, this);
                    clientes.add(handler);
                    handler.start();
                } catch (SocketTimeoutException ignorado) {
                    // Permite revisar el flag de apagado periódicamente.
                }
            }
        } catch (IOException e) {
            if (escuchando) {
                LOG.warning("Error en servidor TCP MEC: " + e.getMessage());
            }
        } finally {
            cerrarSocketServidor();
        }
    }

    public void detener() {
        escuchando = false;
        cerrarSocketServidor();
    }

    private void cerrarSocketServidor() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignorado) { }
        }
    }

    public List<String> getUniversidades() { return universidades; }
    public void eliminar(MecTcpClientHandler handler) { clientes.remove(handler); }
}
