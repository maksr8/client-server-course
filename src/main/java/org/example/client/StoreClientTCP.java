package org.example.client;

import org.example.crypto.EncryptionService;
import org.example.dto.Message;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class StoreClientTCP {
    private final String serverIp;
    private final int port;
    private final EncryptionService encryptionService;
    private final String password;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public StoreClientTCP(String serverIp, int port, EncryptionService encryptionService, String password) {
        this.serverIp = serverIp;
        this.port = port;
        this.encryptionService = encryptionService;
        this.password = password;
    }

    public Message sendRequest(Message request, int maxRetries) throws Exception {
        byte[] encryptedPackage = encryptionService.encrypt(request, password);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ensureConnected();
                out.write(encryptedPackage);
                out.flush();

                byte[] header = new byte[16];
                in.readFully(header);
                if (header[0] != 0x13) {
                    throw new IOException("Invalid magic byte in response");
                }

                int len = ByteBuffer.wrap(header, 10, 4).getInt();
                byte[] payloadAndCrc = new byte[len + 2];
                in.readFully(payloadAndCrc);
                byte[] fullPacket = new byte[16 + payloadAndCrc.length];
                System.arraycopy(header, 0, fullPacket, 0, 16);
                System.arraycopy(payloadAndCrc, 0, fullPacket, 16, payloadAndCrc.length);
                return encryptionService.decrypt(fullPacket, password);
            } catch (IOException e) {
                System.err.println("TCP Client Error (Attempt " + attempt + "): " + e.getMessage());
                closeConnection();
                if (attempt == maxRetries) {
                    throw new RuntimeException("Server is unavailable after " + maxRetries + " attempts");
                }
                Thread.sleep(2000);
            }
        }
        return null;
    }

    private void ensureConnected() throws IOException {
        if (socket == null || socket.isClosed()) {
            System.out.println("TCP Client: Connecting to server...");
            socket = new Socket(serverIp, port);
            socket.setSoTimeout(3000);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        }
    }

    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = null;
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}