package com.company.Logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;


/**
 * Created by ahmadbarakat on 344 / 10 / 15.
 */

public class PacketsManager {

    private static String serverPath;
    protected static int WaitTime;
    protected static int packetSize;
    protected static int windowSize;
    private static double dropRate;
    protected int overAllSize;
    protected int packetCount;
    protected int windowHigh;
    protected int windowLow;
    protected LinkedList<PacketTimer> timers;
    private byte[][] packetsBytes;


    protected PacketsManager(String filePath) throws IOException {
        Path path = Paths.get(serverPath + filePath);
        byte[] allData = Files.readAllBytes(path);
        overAllSize = allData.length;
        int dataPacketHeaderSize = 16;
        int dataPacketDatSize = packetSize - dataPacketHeaderSize;
        packetCount = (int) Math.ceil(((double) overAllSize) / dataPacketDatSize);
        packetsBytes = new byte[packetCount][];
        int i;
        for(i = 0; i < packetCount - 1; ++i) {
            packetsBytes[i] = Arrays.copyOfRange(allData, i * dataPacketDatSize,
                    (i + 1) * dataPacketDatSize);
        }
        packetsBytes[i] = Arrays.copyOfRange(allData, i * dataPacketDatSize, overAllSize);
        if(Networks.mode == Networks.Mode.StopAndWait) {
            windowSize = 1;
        } else if(windowSize == 0) {
            windowSize = (int) Math.ceil(packetCount / 10);
        }
        windowLow = 0;
        windowHigh = windowSize - 1;
        timers = new LinkedList<PacketTimer>();
    }

    public static void setServerPath(String serverPath) {
        PacketsManager.serverPath = serverPath;
    }

    public static void setWaitTime(int waitTime) {
        PacketsManager.WaitTime = waitTime;
    }

    public static void setDropRate(double dropRate) {
        PacketsManager.dropRate = dropRate;
    }

    public static void setPacketSize(int packetSize) {
        PacketsManager.packetSize = packetSize;
    }

    public static void setWindowSize(int windowSize) {
        PacketsManager.windowSize = windowSize;
    }

    protected static int getDropAfter() {
        return (int) (Math.floor(1 / dropRate));
    }

    protected boolean isNextExist() {
        return !((windowHigh == packetCount - 1));
    }

    protected byte[] getPacketAsBytes(int index) {
        return packetsBytes[index];
    }

}
