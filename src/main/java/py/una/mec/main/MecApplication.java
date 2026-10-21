package py.una.mec.main;

import py.una.mec.server.MecTcpServer;
import py.una.mec.server.MecUdpServer;
import py.una.mec.ui.MenuMec;
import py.una.mec.util.LogUtil;

public class MecApplication {
    public static void main(String[] args) {
        final MecTcpServer tcpServer = new MecTcpServer();
        final MecUdpServer udpServer = new MecUdpServer();
        Thread tcpThread = new Thread(tcpServer, "MEC-TCP");
        Thread udpThread = new Thread(udpServer, "MEC-UDP");
        tcpThread.start();
        udpThread.start();
        Thread menuThread = new Thread(new MenuMec(), "MEC-ADMIN");
        menuThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                tcpServer.detener();
                udpServer.detener();
            }
        }));
        LogUtil.get(MecApplication.class).info("MEC listo: TCP 4444 y UDP 9876");
    }
}
