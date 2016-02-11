package com.company.Logic;

import java.util.Timer;


/**
 * Created by ahmadbarakat on 345 / 11 / 15.
 */

public class PacketTimer extends Timer {

    private int sequenceNumber;


    public PacketTimer(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

}
