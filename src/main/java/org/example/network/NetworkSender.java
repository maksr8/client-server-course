package org.example.network;

import java.util.concurrent.BlockingQueue;

public class NetworkSender implements Sender {
    private final BlockingQueue<byte[]> queueToSend;
    private final ConnectionRegistry registry;

    public NetworkSender(BlockingQueue<byte[]> queueToSend, ConnectionRegistry registry) {
        this.queueToSend = queueToSend;
        this.registry = registry;
    }

    @Override
    public void sendMessage() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] packet = queueToSend.take();
                byte clientId = packet[1];
                ClientSession session = registry.getSession(clientId);
                if (session != null) {
                    session.sendData(packet);
                } else {
                    System.err.println("Session not found for client: " + clientId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Send error: " + e.getMessage());
            }
        }
    }
}
