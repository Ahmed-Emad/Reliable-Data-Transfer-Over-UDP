package com.company.Logic;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * Created by ahmadbarakat on 345 / 11 / 15.
 */

public class Packet {

    private PacketType packetType;
    private long checkSum;
    private int overAllSize;
    private int packetSize;
    private int sequenceNumber;
    private int aknNumber;
    private int windowSize;
    private String requestedFile;
    private String clientName;
    private boolean isValidPacket;
    private byte[] dataBytes;
    private byte[] packetBytes;

    public Packet(byte[] packetBytes) {
        this.packetBytes = packetBytes;
        decode(packetBytes);
    }

    public Packet(PacketType packetType, byte[] data, int... options) throws Exception {
        packetBytes = encode(packetType, data, options);
        decode(packetBytes);
    }

    private void decode(byte[] data) {
        short length = ByteBuffer.wrap(data, 0, 2).getShort();
        packetType = PacketType.getMessageType(ByteBuffer.wrap(data, 2, 2).getShort());
        checkSum = ByteBuffer.wrap(data, 4, 8).getLong();
        isValidPacket = true;

        if(packetType == PacketType.Signal) {

            clientName = new String(data, 12, length - 12);

        } else if(packetType == PacketType.FileRequest) {

            requestedFile = new String(data, 12, length - 12);

        } else if(packetType == PacketType.InitializeTransfer) {

            overAllSize = ByteBuffer.wrap(data, 12, 4).getInt();
            packetSize = ByteBuffer.wrap(data, 16, 4).getInt();
            windowSize = ByteBuffer.wrap(data, 20, 4).getInt();

        } else if(packetType == PacketType.DataPacket) {

            sequenceNumber = ByteBuffer.wrap(data, 12, 4).getInt();
            dataBytes = new byte[length - 16];
            dataBytes = Arrays.copyOfRange(data, 16, length);
            isValidPacket = (checkSum == calculateChecksum(dataBytes));

        } else if(packetType == PacketType.AKN) {

            aknNumber = ByteBuffer.wrap(data, 12, 4).getInt();

        }
    }

    private byte[] encode(PacketType packetType, byte[] data, int... numbers) throws Exception {
        short length = 12;
        length += 4 * numbers.length;
        if(data != null) {
            length += data.length;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.putShort(length);
        byteBuffer.putShort(packetType.getValue());
        byteBuffer.putLong(calculateChecksum(data));
        for(int num : numbers) {
            byteBuffer.putInt(num);
        }
        if(data != null) {
            byteBuffer.put(data);
        }
        return byteBuffer.array();
    }

    public long calculateChecksum(byte[] buf) {
        int length = buf != null ? buf.length : 0;
        int i = 0;
        long sum = 0;
        long data;
        // Handle all pairs
        while(length > 1) {
            // Corrected to include @Andy's edits and various comments on Stack Overflow
            data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
            sum += data;
            // 1's complement carry bit correction in 16-bits (detecting sign extension)
            if((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }

            i += 2;
            length -= 2;
        }
        // Handle remaining byte in odd length buffers
        if(length > 0) {
            // Corrected to include @Andy's edits and various comments on Stack Overflow
            sum += (buf[i] << 8 & 0xFF00);
            // 1's complement carry bit correction in 16-bits (detecting sign extension)
            if((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
        }
        // Final 1's complement value correction to 16-bits
        sum = ~sum;
        sum = sum & 0xFFFF;
        return sum;
    }

    public String getClientName() {
        return clientName;
    }

    public boolean isValidPacket() {
        return isValidPacket;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public int getOverAllSize() {
        return overAllSize;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAknNumber() {
        return aknNumber;
    }

    public String getRequestedFile() {
        return requestedFile;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public byte[] getPacketAsBytes() {
        return packetBytes.clone();
    }

    public enum PacketType {
        Signal(0),
        FileRequest(1),
        InitializeTransfer(2),
        DataPacket(3),
        AKN(4);
        private short value;

        PacketType(int value) {
            this.value = (short) value;
        }

        public static PacketType getMessageType(short value) {
            return PacketType.values()[value];
        }

        public short getValue() {
            return value;
        }
    }

}
