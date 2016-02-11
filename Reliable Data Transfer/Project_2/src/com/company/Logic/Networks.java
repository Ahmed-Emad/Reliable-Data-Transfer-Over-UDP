package com.company.Logic;


/**
 * Created by ahmadbarakat on 306 / 2 / 15.
 */

public abstract class Networks implements Runnable {

    protected static Mode mode;
    protected static int portNumber;
    protected Thread thread;


    public static void setPortNumber(int portNumber) {
        Networks.portNumber = portNumber;
    }

    public static void setMode(Networks.Mode mode) {
        Networks.mode = mode;
    }

    public void run() {
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public enum Mode {
        StopAndWait,
        SelectiveRepeat,
        GoBackN;
    }

}
