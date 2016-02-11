package com.company.Logic;

import com.company.Main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;


/**
 * Created by ahmadbarakat on 306 / 2 / 15.
 */

public class UDPClient extends Networks {


    private volatile boolean working;
    private InetAddress IPAddress;
    private DatagramSocket clientSocket;
    private byte[] fileArray;
    private ByteBuffer fileBuffer;
    private LinkedList<Packet> receivedPackets;
    private LinkedList<Integer> receivedSequences;
    private LinkedList<Packet> waitingPackets;
    private LinkedList<Integer> waitingSequences;
    private int currentWriteSequence;
    private int packetSize;
    private int overAllSize;
    private int dropAfter;
    private int packetCount;
    private int windowSize;
    private int windowLow;
    private int windowHigh;
    private int color;
    private String fileName;
    private String path;
    private String name;


    public UDPClient(String name, String path, int color) throws Exception {
        working = true;
        this.path = path;
        this.name = name;
        this.color = color;
        this.packetSize = 1024;
        receivedPackets = new LinkedList<Packet>();
        clientSocket = new DatagramSocket();
        IPAddress = InetAddress.getByName("localhost");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                working = false;
                try {
                    clientSocket.close();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void close() {
        working = false;
    }

    public void run() {
        while(working) {
            byte[] receiveData = new byte[packetSize];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
                readDatagram(receivePacket);
            } catch(Exception ex) {
                System.out.println("??" + name);
                ex.printStackTrace();
                break;
            }
        }
        if(clientSocket != null) {
            clientSocket.close();
        }
    }

    private void sendSignal() throws Exception {
        Packet packet = new Packet(Packet.PacketType.Signal, name.getBytes());
        byte[] packetBytes = packet.getPacketAsBytes();
        DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length,
                IPAddress, portNumber);
        try {
            clientSocket.send(sendPacket);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        Main.printToScreen("CLIENT " + name + " sent Signal", color);
    }

    public void setRequestFile(String fileName) throws Exception {
        this.fileName = fileName;
        sendSignal();
    }

    private void requestFile() throws Exception {
        currentWriteSequence = 0;
        Packet packet = new Packet(Packet.PacketType.FileRequest, fileName.getBytes());
        receivedSequences = new LinkedList<Integer>();
        waitingPackets = new LinkedList<Packet>();
        waitingSequences = new LinkedList<Integer>();

        DatagramPacket sendPacket = new DatagramPacket(packet.getPacketAsBytes(),
                packet.getPacketAsBytes().length, IPAddress, portNumber);
        try {
            clientSocket.send(sendPacket);
        } catch(IOException ex) {
            ex.printStackTrace();
        }

        Main.printToScreen("CLIENT " + name + " requested File: " + fileName, color);
    }

    private void sendAKN(int sequenceNumber) throws Exception {
        Packet packet = new Packet(Packet.PacketType.AKN, null, sequenceNumber);
        byte[] packetBytes = packet.getPacketAsBytes();
        DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length,
                IPAddress, portNumber);
        try {
            clientSocket.send(sendPacket);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        Main.printToScreen("CLIENT " + name + " sent AKN: " + sequenceNumber, color);
        --dropAfter;
    }

    private void writePacket(Packet packet) throws Exception {
        int sequenceNumber = packet.getSequenceNumber();
        if(sequenceNumber == currentWriteSequence) {
            ++currentWriteSequence;
            fileBuffer.put(packet.getDataBytes());
            while(waitingSequences.contains(currentWriteSequence)) {
                int ind = waitingSequences.indexOf(currentWriteSequence);
                fileBuffer.put(waitingPackets.get(ind).getDataBytes());
                waitingPackets.remove(ind);
                waitingSequences.remove(ind);
                ++currentWriteSequence;
            }
        } else {
            waitingSequences.add(packet.getSequenceNumber());
            waitingPackets.add(packet);
        }
        if(receivedSequences.size() == packetCount) {
            writeFile();
        }

    }

    private void writeFile() throws Exception {
        Files.write(Paths.get(path + fileName), fileBuffer.array());
        Main.printToScreen("Client " + name + "  Wrote file to disk", color);
        Main.notifyFinished(name);
    }

    private void readDatagram(DatagramPacket datagramPacket) throws Exception {
        Packet packet = new Packet(datagramPacket.getData());
        String message = "CLIENT " + name + "  received ";
        Packet.PacketType packetType = packet.getPacketType();

        if(packetType == Packet.PacketType.Signal) {
            ///////////////////////////////////////////////////////////////////////////////////////
            Main.printToScreen(message + "Signal", color);
            receivedPackets.add(packet);
            if(packet.getClientName().equals(name)) {
                requestFile();
            }
            ///////////////////////////////////////////////////////////////////////////////////////
        } else if(packetType == Packet.PacketType.FileRequest) {
            ///////////////////////////////////////////////////////////////////////////////////////
            Main.printToScreen(message + "FileRequest: " + packet.getRequestedFile(), color);
            receivedPackets.add(packet);
            ///////////////////////////////////////////////////////////////////////////////////////
        } else if(packetType == Packet.PacketType.InitializeTransfer) {
            ///////////////////////////////////////////////////////////////////////////////////////
            receivedPackets.add(packet);
            packetSize = packet.getPacketSize();
            overAllSize = packet.getOverAllSize();
            int dataPacketHeaderSize = 16;
            int dataPacketDatSize = packetSize - dataPacketHeaderSize;
            packetCount = (int) Math.ceil(((double) overAllSize) / dataPacketDatSize);
            Main.printToScreen(message + "InitializeTransfer: " + packet.getOverAllSize() + ", "
                    + packet.getPacketSize() + ", " + packetCount, color);
            windowSize = packet.getWindowSize();
            Main.printToScreen("Client " + name + "  Window Size: " + windowSize, color);
            windowLow = 0;
            windowHigh = windowSize - 1;
            dropAfter = PacketsManager.getDropAfter();
            Main.printToScreen("Client " + name + "  Drop Packet every: " + dropAfter, color);
            fileArray = new byte[packet.getOverAllSize()];
            fileBuffer = ByteBuffer.wrap(fileArray);
            ///////////////////////////////////////////////////////////////////////////////////////
        } else if(packetType == Packet.PacketType.DataPacket) {
            ///////////////////////////////////////////////////////////////////////////////////////
            int sequenceNumber = packet.getSequenceNumber();
            if(dropAfter != 0 && Networks.mode != Mode.GoBackN && packet.isValidPacket()
                    && sequenceNumber >= windowLow && sequenceNumber <= windowHigh) {
                Main.printToScreen(message + "DataPacket: " + packet.getDataBytes().length + ", "
                        + packet.getSequenceNumber(), color);
                receivedPackets.add(packet);
                sendAKN(sequenceNumber);
                if(!receivedSequences.contains(sequenceNumber)) {
                    receivedSequences.add(sequenceNumber);
                    if(sequenceNumber == windowLow) {
                        while(receivedSequences.contains(windowLow)) {
                            ++windowLow;
                            ++windowHigh;
                        }
                    }
                    writePacket(packet);
                }
            } else if(dropAfter != 0 && Networks.mode == Mode.GoBackN && packet.isValidPacket()) {
                if(sequenceNumber == windowLow) {
                    Main.printToScreen(message + "DataPacket: " + packet.getDataBytes().length + ", "
                            + packet.getSequenceNumber(), color);
                    receivedPackets.add(packet);
                    sendAKN(packet.getSequenceNumber());
                    ++windowLow;
                    if(!receivedSequences.contains(sequenceNumber)) {
                        receivedSequences.add(sequenceNumber);
                        writePacket(packet);
                    }
                } else {
                    if(windowLow - 1 > -1) {
                        sendAKN(windowLow - 1);
                    }
                }
            } else {
                Main.printToScreen(message + "AND dropped out Sequence: " +
                        packet.getSequenceNumber(), color);
                dropAfter = PacketsManager.getDropAfter();
            }
            ///////////////////////////////////////////////////////////////////////////////////////
        } else if(packetType == Packet.PacketType.AKN) {
            ///////////////////////////////////////////////////////////////////////////////////////
            Main.printToScreen(message + "AKN :" + packet.getAknNumber(), color);
            receivedPackets.add(packet);
            ///////////////////////////////////////////////////////////////////////////////////////
        }
    }


}
