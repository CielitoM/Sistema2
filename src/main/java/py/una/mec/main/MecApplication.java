package py.una.mec.main;

import py.una.mec.server.MecTcpServer;
import py.una.mec.server.MecUdpServer;

public class MecApplication {
    public static void main(String[] args) {
        final MecTcpServer tcpServer = new MecTcpServer();
        final MecUdpServer udpServer = new MecUdpServer();
        Thread tcpThread = new Thread(tcpServer, "MEC-TCP");
        Thread udpThread = new Thread(udpServer, "MEC-UDP");
        tcpThread.start();
        udpThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                tcpServer.detener();
                udpServer.detener();
            }
        }));
        System.out.println("MEC listo: TCP 4444 y UDP 9876");
    }
}
