package com.company.Logic;

import java.util.TimerTask;


/**
 * Created by ahmadbarakat on 345 / 11 / 15.
 */

public class PacketTimerTask extends TimerTask {

    private int sequenceNumber;


    public PacketTimerTask(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public void run() {
    }

}