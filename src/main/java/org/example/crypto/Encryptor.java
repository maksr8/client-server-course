package org.example.crypto;

import org.example.dto.Message;

import java.util.concurrent.BlockingQueue;

public class Encryptor {
    private final BlockingQueue<Message> queueToEncrypt;
    private final BlockingQueue<byte[]> queueToSend;
    private final EncryptionService encryptionService;
    private final String secretPassword;

    public Encryptor(BlockingQueue<Message> queueToEncrypt, BlockingQueue<byte[]> queueToSend, EncryptionService encryptionService, String secretPassword) {
        this.queueToEncrypt = queueToEncrypt;
        this.queueToSend = queueToSend;
        this.encryptionService = encryptionService;
        this.secretPassword = secretPassword;
    }

    public void encrypt() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message message = queueToEncrypt.take();
                byte[] encryptedPackage = encryptionService.encrypt(message, secretPassword);
                queueToSend.put(encryptedPackage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Encryption error: " + e.getMessage());
            }
        }
    }
}