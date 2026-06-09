package org.example.network;

import org.example.crypto.EncryptionService;
import org.example.dto.Message;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class ReceiverFake implements Receiver {
    private final BlockingQueue<byte[]> queueToDecrypt;
    private final int messageAmount;
    private final EncryptionService encryptionService;
    private final String secretPassword;
    private final Random random = new Random();
    private long messageIDCounter = 0;

    public ReceiverFake(BlockingQueue<byte[]> queueToDecrypt, int messageAmount, EncryptionService encryptionService, String secretPassword) {
        this.queueToDecrypt = queueToDecrypt;
        this.messageAmount = messageAmount;
        this.encryptionService = encryptionService;
        this.secretPassword = secretPassword;
    }

    @Override
    public void receiveMessage() {
        for (int i = 0; i < messageAmount; i++) {
            try {
                byte[] packageBytes = generateRandomPackage();
                queueToDecrypt.put(packageBytes);
                Thread.sleep(random.nextInt(100) + 100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Fake package generation error: " + e.getMessage());
            }
        }
    }

    private byte[] generateRandomPackage() throws Exception {
        int commandType = random.nextInt(3) + 1;
        int userId = random.nextInt(5) + 1;
        int productId = random.nextInt(2) + 1;

        String payload;
        if (commandType == 1) {
            payload = String.valueOf(productId);
        } else {
            int amount = random.nextInt(10) + 1;
            payload = productId + ":" + amount;
        }

        Message message = new Message((byte) 0x01, messageIDCounter++, commandType, userId, payload);
        return encryptionService.encrypt(message, secretPassword);
    }
}
