package org.example.processor;

import org.example.crypto.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Processor {
    private final BlockingQueue<Message> queueToProcess;
    private final BlockingQueue<Message> queueToEncrypt;
    private final ConcurrentMap<Integer, AtomicInteger> storage;

    public Processor(BlockingQueue<Message> queueToProcess, BlockingQueue<Message> queueToEncrypt, ConcurrentMap<Integer, AtomicInteger> storage) {
        this.queueToProcess = queueToProcess;
        this.queueToEncrypt = queueToEncrypt;
        this.storage = storage;
    }

    public void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message message = queueToProcess.take();
                Message responseMessage = processOneMessage(message);
                queueToEncrypt.put(responseMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Processing error: " + e.getMessage());
            }
        }
    }

    private Message processOneMessage(Message message) {
        int command = message.commandType();
        int userId = message.userID();
        String payload = message.messageString();
        String responseText;
        int productId;
        
        try {
            if (command == 1) {
                productId = Integer.parseInt(payload);
                AtomicInteger stock = storage.get(productId);
                int current = (stock != null) ? stock.get() : 0;
                responseText = "Stock for product [" + productId + "]: " + current;
            } else if (command == 2 || command == 3) {
                String[] parts = payload.split(":");
                productId = Integer.parseInt(parts[0]);
                int amount = Integer.parseInt(parts[1]);

                storage.putIfAbsent(productId, new AtomicInteger(0));
                if (command == 2) {
                    int afterReduce = storage.get(productId).addAndGet(-amount);
                    responseText = "Reduced " + amount + ". New stock [" + productId + "]: " + afterReduce;
                } else {
                    int afterAdd = storage.get(productId).addAndGet(amount);
                    responseText = "Added " + amount + ". New stock [" + productId + "]: " + afterAdd;
                }
            } else {
                responseText = "Unknown command: " + command;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            responseText = "Invalid payload format: " + payload;
        }

        return new Message(message.clientAppNumber(), message.messageID(), 0, userId, responseText);
    }
}