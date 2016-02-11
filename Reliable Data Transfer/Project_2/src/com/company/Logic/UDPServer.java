package com.company.Logic;

import com.company.Main;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


/**
 * Created by ahmadbarakat on 306 / 2 / 15.
 */

public class UDPServer extends Networks {


    private static volatile boolean working;
    private DatagramSocket serverSocket;
    private LinkedList<SocketAddress> clients;
    private LinkedList<Responder> responders;


    public UDPServer() {
        working = true;
        clients = new LinkedList<SocketAddress>();
        responders = new LinkedList<Responder>();
        try {
            serverSocket = new DatagramSocket(portNumber);
        } catch(SocketException ex) {
            ex.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                working = false;
                try {
                    serverSocket.close();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void closeUdpServers() {
        UDPServer.working = false;
        Thread.currentThread().interrupt();
    }

    public void run() {
        while(UDPServer.working) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                serverSocket.receive(receivePacket);
                Responder responder = figureOutClient(receivePacket);
                readDatagram(receivePacket);
                responder.handleMessage(receivePacket);
            } catch(Exception ex) {
                ex.printStackTrace();
                break;
            }
        }
        if(serverSocket != null) {
            serverSocket.close();
        }
    }

    private Responder figureOutClient(DatagramPacket packet) {
        int index;
        if((index = clients.indexOf(packet.getSocketAddress())) != -1) {
            return responders.get(index);
        } else {
            Main.printToScreen("New Responder Created", 2);
            clients.add(packet.getSocketAddress());
            Responder responder = new Responder(serverSocket, packet);
            responders.add(responder);
            return responder;
        }
    }

    private void readDatagram(DatagramPacket datagramPacket) throws Exception {
        Packet packet = new Packet(datagramPacket.getData());
        String message = "SERVER received ";

        if(packet.getPacketType() == Packet.PacketType.Signal) {
            Main.printToScreen(message + "Signal", 2);
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////////////

    public class Responder {

        private DatagramSocket datagramSocket;
        private DatagramPacket datagramPacket;
        private InetAddress IPAddress;
        private String name;
        private int port;
        private PacketsManager packetsManager;
        private LinkedList<Integer> receivedAkns;
        private LinkedList<Packet> receivedPackets;

        public Responder(DatagramSocket datagramSocket, DatagramPacket packet) {
            this.datagramSocket = datagramSocket;
            this.datagramPacket = packet;
            this.IPAddress = datagramPacket.getAddress();
            this.port = datagramPacket.getPort();
            receivedPackets = new LinkedList<Packet>();
            receivedAkns = new LinkedList<Integer>();
        }

        public void sendSignal() throws Exception {
            Packet packet = new Packet(Packet.PacketType.Signal, name.getBytes());
            byte[] packetBytes = packet.getPacketAsBytes();
            final DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length,
                    IPAddress, port);
            try {
                datagramSocket.send(sendPacket);
            } catch(IOException ex) {
                ex.printStackTrace();
            }
            Main.printToScreen("Responder " + name + "  sent Signal", 2);
        }

        private void sendInitialization(int dataSize, int packetSize) throws Exception {
            Packet packet = new Packet(Packet.PacketType.InitializeTransfer, null, dataSize,
                    packetSize, PacketsManager.windowSize);
            byte[] packetBytes = packet.getPacketAsBytes();
            final DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length,
                    IPAddress, port);
            try {
                datagramSocket.send(sendPacket);
            } catch(IOException ex) {
                ex.printStackTrace();
            }
            Main.printToScreen("Responder " + name + "  sent InitializeTransfer: " + dataSize +
                    ", " + packetSize + ", " + packetsManager.packetCount, 2);
        }

        private void sendPacket(byte[] data, final int sequenceNumber) throws Exception {
            if(working && !receivedAkns.contains(sequenceNumber)) {
                Packet packet = new Packet(Packet.PacketType.DataPacket, data, sequenceNumber);
                byte[] packetBytes = packet.getPacketAsBytes();
                final DatagramPacket sendPacket = new DatagramPacket(packetBytes,
                        packetBytes.length, IPAddress, port);
                try {
                    datagramSocket.send(sendPacket);
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
                Main.printToScreen("Responder " + name + "  sent DataPacket: " + data.length
                        + ", " + sequenceNumber, 2);
                if(Networks.mode != Mode.GoBackN) {
                    PacketTimer timer = new PacketTimer(sequenceNumber);
                    PacketTimerTask action = new PacketTimerTask(sequenceNumber) {
                        public void run() {
                            int timerSequenceNumber = this.getSequenceNumber();
                            if(!receivedAkns.contains(timerSequenceNumber)) {
                                try {
                                    Main.printToScreen("Failed To Send: " + timerSequenceNumber, 2);
                                    sendPacket(packetsManager.getPacketAsBytes(timerSequenceNumber),
                                            timerSequenceNumber);
                                    this.cancel();
                                } catch(Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    };
                    timer.schedule(action, PacketsManager.WaitTime);
                    packetsManager.timers.addLast(timer);
                }
            } else {
                Main.printToScreen("Server Closed", 2);
            }
        }

        private synchronized void addGoBackNTimer(int sequenceNumber) {
            if(Networks.mode == Mode.GoBackN && sequenceNumber == packetsManager.windowLow) {
                PacketTimer timer = new PacketTimer(sequenceNumber);
                PacketTimerTask action = new PacketTimerTask(sequenceNumber) {
                    public void run() {
                        int timerSequenceNumber = this.getSequenceNumber();
                        if(!receivedAkns.contains(timerSequenceNumber)) {
                            try {
                                Main.printToScreen("Failed To Send Window From: " +
                                        timerSequenceNumber, 2);
                                int low = packetsManager.windowLow;
                                int high = packetsManager.windowHigh;
                                addGoBackNTimer(low);
                                for(int i = low; i <= high; ++i) {
                                    sendPacket(packetsManager.getPacketAsBytes(i), i);
                                }
                                this.cancel();
                            } catch(Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                };
                timer.schedule(action, PacketsManager.WaitTime);
                packetsManager.timers.addLast(timer);
            }
        }

        private synchronized void deleteTimer(int aknNumber) {
            for(int i = 0; i < packetsManager.timers.size(); ++i) {
                if(packetsManager.timers.get(i).getSequenceNumber() == aknNumber) {
                    packetsManager.timers.remove(i).cancel();
                }
            }
        }

        private synchronized void sendNextPacket(int aknNumber) throws Exception {
            if(Networks.mode != Mode.GoBackN) {
                if(aknNumber == packetsManager.windowLow) {
                    while(packetsManager.isNextExist() && receivedAkns.contains(
                            packetsManager.windowLow)) {
                        ++packetsManager.windowLow;
                        ++packetsManager.windowHigh;
                        sendPacket(packetsManager.getPacketAsBytes(packetsManager.windowHigh),
                                packetsManager.windowHigh);
                    }
                }
                if(receivedAkns.size() == packetsManager.packetCount) {
                    Main.printToScreen("Packets Count: " + receivedPackets.size(), 2);
                }
            } else if(Networks.mode == Mode.GoBackN) {
                packetsManager.windowLow = aknNumber + 1;
                addGoBackNTimer(packetsManager.windowLow);
                while(packetsManager.windowHigh < (packetsManager.packetCount - 1)
                        && packetsManager.windowHigh < (packetsManager.windowLow
                        + PacketsManager.windowSize - 1)) {
                    ++packetsManager.windowHigh;
                    sendPacket(packetsManager.getPacketAsBytes(packetsManager.windowHigh),
                            packetsManager.windowHigh);
                }
                Main.printToScreen("Low: " + packetsManager.windowLow + ", High: " +
                        packetsManager.windowHigh, 2);
            }

        }

        protected void handleMessage(DatagramPacket datagramPacket) throws Exception {
            Packet packet = new Packet(datagramPacket.getData());
            String message = "Responder " + name + " received ";
            Packet.PacketType packetType = packet.getPacketType();

            if(packetType == Packet.PacketType.Signal) {
                ///////////////////////////////////////////////////////////////////////////////////
                Main.printToScreen(message + "Signal", 2);
                this.name = packet.getClientName();
                receivedPackets = new LinkedList<Packet>();
                receivedAkns = new LinkedList<Integer>();
                receivedPackets.add(packet);
                sendSignal();
                ///////////////////////////////////////////////////////////////////////////////////
            } else if(packetType == Packet.PacketType.FileRequest) {
                ///////////////////////////////////////////////////////////////////////////////////
                Main.printToScreen(message + "File Request: " + packet.getRequestedFile(), 2);
                packetsManager = new PacketsManager(packet.getRequestedFile());
                receivedPackets.add(packet);
                sendInitialization(packetsManager.overAllSize, PacketsManager.packetSize);
                if(Networks.mode == Mode.GoBackN) {
                    addGoBackNTimer(packetsManager.windowLow);
                }
                for(int i = 0; i <= packetsManager.windowHigh; ++i) {
                    sendPacket(packetsManager.getPacketAsBytes(i), i);
                }
                ///////////////////////////////////////////////////////////////////////////////////
            } else if(packetType == Packet.PacketType.InitializeTransfer) {
                ///////////////////////////////////////////////////////////////////////////////////
                Main.printToScreen(message + "InitializeTransfer", 2);
                receivedPackets.add(packet);
                ///////////////////////////////////////////////////////////////////////////////////
            } else if(packetType == Packet.PacketType.DataPacket) {
                ///////////////////////////////////////////////////////////////////////////////////
                Main.printToScreen(message + "DataPacket", 2);
                receivedPackets.add(packet);
                ///////////////////////////////////////////////////////////////////////////////////
            } else if(packetType == Packet.PacketType.AKN) {
                ///////////////////////////////////////////////////////////////////////////////////
                int aknNumber = packet.getAknNumber();
                Main.printToScreen(message + "AKN : " + aknNumber, 2);
                receivedPackets.add(packet);
                if(Networks.mode != Mode.GoBackN) {
                    deleteTimer(aknNumber);
                    if(aknNumber >= packetsManager.windowLow && aknNumber <=
                            packetsManager.windowHigh && !receivedAkns.contains(aknNumber)) {
                        receivedAkns.add(aknNumber);
                        sendNextPacket(aknNumber);
                    }
                } else if(mode == Mode.GoBackN) {
                    if(aknNumber >= packetsManager.windowLow) {
                        for(int i = packetsManager.windowLow; i <= aknNumber; ++i) {
                            receivedAkns.add(i);
                        }
                        deleteTimer(packetsManager.windowLow);
                        sendNextPacket(aknNumber);
                    }
                }
                ///////////////////////////////////////////////////////////////////////////////////
            }
        }

    }


}
