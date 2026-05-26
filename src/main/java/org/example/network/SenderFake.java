package org.example.network;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class SenderFake implements Sender {
    private final BlockingQueue<byte[]> queueToSend;

    public SenderFake(BlockingQueue<byte[]> queueToSend) {
        this.queueToSend = queueToSend;
    }

    @Override
    public void sendMessage() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] packageToSend = queueToSend.take();
                System.out.println("Sent package: " + Arrays.toString(packageToSend));
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Sending error: " + e.getMessage());
            }
        }
    }
}