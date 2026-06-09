package org.example.client;

import org.example.crypto.EncryptionService;
import org.example.dto.Message;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class StoreClientUDP {
    private final String serverIp;
    private final int port;
    private final EncryptionService encryptionService;
    private final String password;

    public StoreClientUDP(String serverIp, int port, EncryptionService encryptionService, String password) {
        this.serverIp = serverIp;
        this.port = port;
        this.encryptionService = encryptionService;
        this.password = password;
    }

    public Message sendRequest(Message request, int maxRetries) throws Exception {
        byte[] encryptedPackage = encryptionService.encrypt(request, password);
        InetAddress address = InetAddress.getByName(serverIp);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);
            DatagramPacket sendPacket = new DatagramPacket(encryptedPackage, encryptedPackage.length, address, port);

            byte[] buffer = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    socket.send(sendPacket);
                    socket.receive(receivePacket); // with timeout we set
                    byte[] responseData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    return encryptionService.decrypt(responseData, password);
                } catch (SocketTimeoutException e) {
                    System.err.println("UDP Client: Packet lost. Retrying (Attempt " + attempt + ")");
                    if (attempt == maxRetries) {
                        throw new RuntimeException("UDP Server did not respond after " + maxRetries + " attempts");
                    }
                }
            }
        }
        return null;
    }
}