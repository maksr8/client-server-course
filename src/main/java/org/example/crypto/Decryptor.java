package org.example.crypto;

import org.example.dto.Message;

import java.util.concurrent.BlockingQueue;

public class Decryptor {
    private final BlockingQueue<byte[]> queueToDecrypt;
    private final BlockingQueue<Message> queueToProcess;
    private final EncryptionService encryptionService;
    private final String secretPassword;

    public Decryptor(BlockingQueue<byte[]> queueToDecrypt, BlockingQueue<Message> queueToProcess, EncryptionService encryptionService, String secretPassword) {
        this.queueToDecrypt = queueToDecrypt;
        this.queueToProcess = queueToProcess;
        this.encryptionService = encryptionService;
        this.secretPassword = secretPassword;
    }

    public void decrypt() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] message = queueToDecrypt.take();
                Message decrypted = encryptionService.decrypt(message, secretPassword);
                queueToProcess.put(decrypted);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Decryption error: " + e.getMessage());
            }
        }
    }
}
