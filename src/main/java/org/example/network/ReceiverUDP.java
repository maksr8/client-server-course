package org.example.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class ReceiverUDP implements Receiver {
    private final int port;
    private final BlockingQueue<byte[]> queueToDecrypt;
    private final ConnectionRegistry registry;

    public ReceiverUDP(int port, BlockingQueue<byte[]> queueToDecrypt, ConnectionRegistry registry) {
        this.port = port;
        this.queueToDecrypt = queueToDecrypt;
        this.registry = registry;
    }

    @Override
    public void receiveMessage() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(500);
            System.out.println("UDP Server started on port " + port);
            byte[] buffer = new byte[4096];
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                    socket.receive(datagram);
                    byte[] packetData = Arrays.copyOf(datagram.getData(), datagram.getLength());
                    if (packetData.length < 16 || packetData[0] != 0x13) {
                        System.err.println("UDP: Invalid packet dropped from " + datagram.getSocketAddress());
                        continue;
                    }
                    byte clientId = packetData[1];
                    SocketAddress clientAddress = datagram.getSocketAddress();
                    ClientSessionUDP session = new ClientSessionUDP(socket, clientAddress);
                    registry.register(clientId, session);
                    queueToDecrypt.put(packetData);
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("UDP Server error: " + e.getMessage());
            }
        }
    }
}