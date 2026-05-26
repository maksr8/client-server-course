package org.example;

import org.assertj.core.api.Assertions;
import org.example.crypto.Decryptor;
import org.example.crypto.EncryptionService;
import org.example.crypto.Encryptor;
import org.example.crypto.Message;
import org.example.network.ReceiverFake;
import org.example.network.SenderFake;
import org.example.processor.Processor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class ConcurrencyTest {

    @Test
    void testConcurrencyWorkAndTerminateCorrectly() throws InterruptedException {
        BlockingQueue<byte[]> queueToDecrypt = new LinkedBlockingQueue<>(1000);
        BlockingQueue<Message> queueToProcess = new LinkedBlockingQueue<>(1000);
        BlockingQueue<Message> queueToEncrypt = new LinkedBlockingQueue<>(1000);
        BlockingQueue<byte[]> queueToSend = new LinkedBlockingQueue<>(1000);

        ConcurrentMap<Integer, AtomicInteger> storage = new ConcurrentHashMap<>();
        storage.put(1, new AtomicInteger(252));
        storage.put(2, new AtomicInteger(441));

        EncryptionService encryptionService = new EncryptionService();
        String password = "My SUPER secret cybersecure password! cyberops analyst!";

        ExecutorService executor = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 2; i++) {
            ReceiverFake receiver = new ReceiverFake(queueToDecrypt, 100, encryptionService, password);
            executor.submit(receiver::receiveMessage);
        }
        for (int i = 0; i < 2; i++) {
            Decryptor decryptor = new Decryptor(queueToDecrypt, queueToProcess, encryptionService, password);
            executor.submit(decryptor::decrypt);
        }
        for (int i = 0; i < 4; i++) {
            Processor processor = new Processor(queueToProcess, queueToEncrypt, storage);
            executor.submit(processor::processMessages);
        }
        for (int i = 0; i < 3; i++) {
            Encryptor encryptor = new Encryptor(queueToEncrypt, queueToSend, encryptionService, password);
            executor.submit(encryptor::encrypt);
        }
        for (int i = 0; i < 5; i++) {
            SenderFake sender = new SenderFake(queueToSend);
            executor.submit(sender::sendMessage);
        }

        Thread.sleep(10000);
        executor.shutdownNow();
        boolean terminated = executor.awaitTermination(2, TimeUnit.SECONDS);

        Assertions.assertThat(terminated)
                .as("All threads should successfully terminate")
                .isTrue();

        Assertions.assertThat(storage).isNotEmpty();
        Assertions.assertThat(storage.get(1)).isNotNull();
        Assertions.assertThat(storage.get(2)).isNotNull();

        System.out.println("Final storage state: " + storage);
    }

    @Test
    void testProcessorConcurrencyWithExactResult() throws InterruptedException {
        BlockingQueue<Message> queueToProcess = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queueToEncrypt = new LinkedBlockingQueue<>();

        ConcurrentMap<Integer, AtomicInteger> storage = new ConcurrentHashMap<>();
        storage.put(1, new AtomicInteger(100));

        for (int i = 0; i < 998; i++) {
            queueToProcess.put(new Message((byte) 1, i, 3, 1, "1:10"));
            queueToProcess.put(new Message((byte) 1, i + 1000, 2, 2, "1:5"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            Processor processor = new Processor(queueToProcess, queueToEncrypt, storage);
            executor.submit(processor::processMessages);
        }

        while (queueToEncrypt.size() < 1996) {
            Thread.sleep(50);
        }

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // 100 + (998 * 10) - (998 * 5) = 5090
        int finalStock = storage.get(1).get();
        System.out.println("Expected: 5090, real: " + finalStock);

        Assertions.assertThat(finalStock).isEqualTo(5090);
    }
}