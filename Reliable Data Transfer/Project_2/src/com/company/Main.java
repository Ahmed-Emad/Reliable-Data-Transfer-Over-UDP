package com.company;

import com.company.Logic.Networks;
import com.company.Logic.PacketsManager;
import com.company.Logic.UDPClient;
import com.company.Logic.UDPServer;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by ahmadbarakat on 306 / 2 / 15.
 */

public class Main {

    public static final String ANSI_CYAN = "\u001B[36m";    //0
    public static final String ANSI_PURPLE = "\u001B[35m";  //1
    public static final String ANSI_BLUE = "\u001B[34m";    //2
    public static final String ANSI_GREEN = "\u001B[32m";   //3
    public static final String ANSI_RED = "\u001B[31m";     //4
    public static final String ANSI_YELLOW = "\u001B[33m";  //5
    public static final String ANSI_BLACK = "\u001B[30m";
    private static final Object syncObj = new Object();
    private static Map<String, UDPClient> clients;
    private static Map<String, Long> timers;
    private static Map<String, Long> elapsed;
    private static int clientsWaiting;

    public static void main(String[] args) throws Exception {

        System.out.println();
        clients = new HashMap<String, UDPClient>();
        timers = new HashMap<String, Long>();
        elapsed = new HashMap<String, Long>();
        clientsWaiting = 0;

        Networks.setPortNumber(7080);
        Networks.setMode(Networks.Mode.GoBackN);
        PacketsManager.setServerPath("/Users/ahmadbarakat/Desktop/Server/");
        PacketsManager.setWindowSize(20);
        PacketsManager.setPacketSize(1000);
        PacketsManager.setDropRate(0.07);
        PacketsManager.setWaitTime(900);

        UDPServer server = new UDPServer();
        server.start();

        String client1Name = "AHM";
        UDPClient client1 = new UDPClient(client1Name, "/Users/ahmadbarakat/Desktop/Client1/", 3);
        timers.put(client1Name, System.currentTimeMillis());
        client1.start();
        client1.setRequestFile("file7.jpg");
        clients.put(client1Name, client1);
        ++clientsWaiting;

//        String client2Name = "MAL";
//        UDPClient client2 = new UDPClient(client2Name, "/Users/ahmadbarakat/Desktop/Client2/", 4);
//        timers.put(client2Name, System.currentTimeMillis());
//        client2.start();
//        client2.setRequestFile("file6.pdf");
//        clients.put(client2Name, client2);
//        ++clientsWaiting;
//
//        String client3Name = "ATE";
//        UDPClient client3 = new UDPClient(client3Name, "/Users/ahmadbarakat/Desktop/Client3/", 5);
//        timers.put(client3Name, System.currentTimeMillis());
//        client3.start();
//        client3.setRequestFile("file3.jpg");
//        clients.put(client3Name, client3);
//        ++clientsWaiting;

    }

    public static void notifyFinished(String name) throws InterruptedException {
        long lEndTime = System.currentTimeMillis();
        long difference = lEndTime - timers.get(name);
        elapsed.put(name, difference);
        clients.get(name).close();
        --clientsWaiting;

        if(clientsWaiting == 0) {
            UDPServer.closeUdpServers();
            printToScreen("\n", 0);
            String starsLine = "";
            for(int i = 0; i < 50; ++i) {
                starsLine += (ANSI_CYAN + '*' + ANSI_CYAN);
            }

            for(String key : elapsed.keySet()) {
                String line2 = "  " + key + " FINISHED with Elapsed milliseconds: "
                        + elapsed.get(key);
                printToScreen(starsLine, 0);
                printToScreen(line2, 1);
                printToScreen(starsLine + "\n", 0);
            }
            System.exit(0);
        }
    }

    public synchronized static void printToScreen(String message, int color) {
        synchronized(syncObj) {
            switch(color) {
                case 0:
                    System.out.println(ANSI_CYAN + message + ANSI_CYAN);
                    break;
                case 1:
                    System.out.println(ANSI_PURPLE + message + ANSI_PURPLE);
                    break;
                case 2:
                    System.out.println(ANSI_BLUE + message + ANSI_BLUE);
                    break;
                case 3:
                    System.out.println(ANSI_GREEN + message + ANSI_GREEN);
                    break;
                case 4:
                    System.out.println(ANSI_RED + message + ANSI_RED);
                    break;
                case 5:
                    System.out.println(ANSI_YELLOW + message + ANSI_YELLOW);
                    break;
            }
        }
    }

}
