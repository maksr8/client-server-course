package org.example.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class ReceiverTCP implements Receiver {
    private final int port;
    private final BlockingQueue<byte[]> queueToDecrypt;
    private final ConnectionRegistry registry;
    private final ExecutorService executor;

    public ReceiverTCP(int port, BlockingQueue<byte[]> queueToDecrypt, ConnectionRegistry registry, ExecutorService executor) {
        this.port = port;
        this.queueToDecrypt = queueToDecrypt;
        this.registry = registry;
        this.executor = executor;
    }

    @Override
    public void receiveMessage() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP Server started on port " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("TCP: New connection from " + clientSocket.getRemoteSocketAddress());
                ClientSessionTCP session = new ClientSessionTCP(clientSocket, registry, queueToDecrypt);
                executor.submit(session::listen);
            }
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("TCP Server error: " + e.getMessage());
            }
        }
    }
}