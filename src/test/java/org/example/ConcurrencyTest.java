package org.example;

import org.assertj.core.api.Assertions;
import org.example.crypto.Decryptor;
import org.example.crypto.EncryptionService;
import org.example.crypto.Encryptor;
import org.example.dto.Message;
import org.example.model.Item;
import org.example.network.ReceiverFake;
import org.example.network.SenderFake;
import org.example.processor.Processor;
import org.example.repository.ItemRepository;
import org.example.service.ItemService;
import org.example.service.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.*;

class ConcurrencyTest extends BasePostgresqlTest{

    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() throws Exception {
        itemRepository = new ItemRepository(connectionProvider);
        itemService = new ItemServiceImpl(itemRepository);

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE items RESTART IDENTITY");
        }
    }

    @Test
    void testConcurrencyWorkAndTerminateCorrectly() throws InterruptedException {
        BlockingQueue<byte[]> queueToDecrypt = new LinkedBlockingQueue<>(1000);
        BlockingQueue<Message> queueToProcess = new LinkedBlockingQueue<>(1000);
        BlockingQueue<Message> queueToEncrypt = new LinkedBlockingQueue<>(1000);
        BlockingQueue<byte[]> queueToSend = new LinkedBlockingQueue<>(1000);

        itemRepository.create(new Item(null, "Item1", "Category", 10.0, 252));
        itemRepository.create(new Item(null, "Item2", "Category", 20.0, 441));

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
            Processor processor = new Processor(queueToProcess, queueToEncrypt, itemService);
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

        Item item1 = itemRepository.findById(1);
        Item item2 = itemRepository.findById(2);
        Assertions.assertThat(item1).isNotNull();
        Assertions.assertThat(item2).isNotNull();

        System.out.println("Final storage state - Item 1: " + item1.getQuantity() + ", Item 2: " + item2.getQuantity());
    }

    @Test
    void testProcessorConcurrencyWithExactResult() throws InterruptedException {
        BlockingQueue<Message> queueToProcess = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queueToEncrypt = new LinkedBlockingQueue<>();

        itemRepository.create(new Item(null, "Item1", "Category", 10.0, 100));

        for (int i = 0; i < 998; i++) {
            queueToProcess.put(new Message((byte) 1, i, 3, 1, "{\"id\":1, \"amount\":10}"));
            queueToProcess.put(new Message((byte) 1, i + 1000, 2, 2, "{\"id\":1, \"amount\":5}"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            Processor processor = new Processor(queueToProcess, queueToEncrypt, itemService);
            executor.submit(processor::processMessages);
        }

        while (queueToEncrypt.size() < 1996) {
            Thread.sleep(50);
        }

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // 100 + (998 * 10) - (998 * 5) = 5090
        int finalStock = itemRepository.findById(1).getQuantity();
        System.out.println("Expected: 5090, real: " + finalStock);

        Assertions.assertThat(finalStock).isEqualTo(5090);
    }
}