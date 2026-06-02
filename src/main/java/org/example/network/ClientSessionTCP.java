package org.example.network;

import java.io.DataInputStream;
import java.io.EOFException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class ClientSessionTCP implements ClientSession {
    private static final int MAX_PAYLOAD_SIZE = 5 * 1024 * 1024; // 5 MB
    
    private final Socket socket;
    private final ConnectionRegistry registry;
    private final BlockingQueue<byte[]> queueToDecrypt;
    private byte clientId = -1;

    public ClientSessionTCP(Socket socket, ConnectionRegistry registry, BlockingQueue<byte[]> queueToDecrypt) {
        this.socket = socket;
        this.registry = registry;
        this.queueToDecrypt = queueToDecrypt;
    }

    @Override
    public void sendData(byte[] data) throws Exception {
        synchronized (socket.getOutputStream()) {
            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();
        }
    }

    public void listen() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                byte[] header = new byte[16];
                in.readFully(header);
                if (header[0] != 0x13) {
                    System.err.println("TCP: Invalid magic byte.");
                    break;
                }
                clientId = header[1];
                registry.register(clientId, this);

                int len = ByteBuffer.wrap(header, 10, 4).getInt();
                if (len < 0 || len > MAX_PAYLOAD_SIZE) {
                    System.err.println("TCP: Invalid payload length: " + len);
                    break;
                }

                byte[] payloadAndCrc = new byte[len + 2];
                in.readFully(payloadAndCrc);
                byte[] fullPacket = new byte[16 + len + 2];
                System.arraycopy(header, 0, fullPacket, 0, 16);
                System.arraycopy(payloadAndCrc, 0, fullPacket, 16, payloadAndCrc.length);
                queueToDecrypt.put(fullPacket);
            }
        } catch (EOFException e) {
            System.out.println("TCP: Client [" + clientId + "] disconnected normally.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("TCP: Listen error for client [" + clientId + "]: " + e.getMessage());
        } finally {
            closeSession();
        }
    }

    private void closeSession() {
        if (clientId != -1) {
            registry.remove(clientId);
        }
        try {
            if (!socket.isClosed()) socket.close();
        } catch (Exception e) {
            System.err.println("TCP: Error closing socket: " + e.getMessage());
        }
    }
}