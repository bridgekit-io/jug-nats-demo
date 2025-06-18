package io.bridgekit.nats;

import java.util.concurrent.CountDownLatch;

/**
 * A quick and dirty helper thread that listens for the user to press the ENTER key. It provides
 * methods to let you create a "while enter not pressed" loop as well as the synchronization logic
 * to "block until enter pressed".
 */
public class EnterListener {
    private final Thread mainThread;
    private final CountDownLatch shutdownLatch;

    public EnterListener() {
        mainThread = Thread.currentThread();
        shutdownLatch = new CountDownLatch(1);

        new Thread(() -> {
            System.console().readLine();
            shutdownLatch.countDown();
            mainThread.interrupt();
        }).start();
    }

    /**
     * Returns true if the user has NOT pressed the Enter key yet.
     */
    public boolean notPressed() {
        return shutdownLatch.getCount() > 0;
    }

    /**
     * Blocks until the user hits the Enter key... forever.
     */
    public void awaitPressed() {
        try {
            shutdownLatch.await();
        }
        catch (InterruptedException e) {
            // This is just for demo code, don't worry about graceful error handling!
        }
    }
}
