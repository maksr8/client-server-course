package org.example.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class ClientSessionUDP implements ClientSession {
    private final DatagramSocket serverSocket;
    private final SocketAddress clientAddress;

    public ClientSessionUDP(DatagramSocket serverSocket, SocketAddress clientAddress) {
        this.serverSocket = serverSocket;
        this.clientAddress = clientAddress;
    }

    @Override
    public void sendData(byte[] data) throws Exception {
        DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress);
        serverSocket.send(packet);
    }
}