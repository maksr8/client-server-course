package org.example;

import org.assertj.core.api.Assertions;
import org.example.client.StoreClientTCP;
import org.example.client.StoreClientUDP;
import org.example.crypto.Decryptor;
import org.example.crypto.EncryptionService;
import org.example.crypto.Encryptor;
import org.example.dto.Message;
import org.example.model.Item;
import org.example.network.ConnectionRegistry;
import org.example.network.NetworkSender;
import org.example.network.ReceiverTCP;
import org.example.network.ReceiverUDP;
import org.example.processor.Processor;
import org.example.repository.ItemRepository;
import org.example.service.ItemService;
import org.example.service.ItemServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class NetworkTest extends BasePostgresqlTest{
    private static final String SERVER_IP = "localhost";
    private static final int TCP_PORT = 8081;
    private static final int UDP_PORT = 8082;
    private static final String PASSWORD = "My SUPER secret cybersecure password! cyberops analyst!";
    
    private ExecutorService serverExecutor;
    private EncryptionService encryptionService;
    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void startServer() throws SQLException {
        encryptionService = new EncryptionService();
        itemRepository = new ItemRepository(connectionProvider);
        itemService = new ItemServiceImpl(itemRepository);
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE items RESTART IDENTITY");
        }
        itemRepository.create(new Item(null, "Item1", "Category", 10.0, 100));
        itemRepository.create(new Item(null, "Item2", "Category", 20.0, 500));

        ConnectionRegistry registry = new ConnectionRegistry();
        BlockingQueue<byte[]> rawQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<Message> decryptedQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<Message> responseQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<byte[]> outgoingQueue = new LinkedBlockingQueue<>(100);

        serverExecutor = Executors.newCachedThreadPool();
        serverExecutor.submit(new ReceiverTCP(TCP_PORT, rawQueue, registry, serverExecutor)::receiveMessage);
        serverExecutor.submit(new ReceiverUDP(UDP_PORT, rawQueue, registry)::receiveMessage);
        serverExecutor.submit(new Decryptor(rawQueue, decryptedQueue, encryptionService, PASSWORD)::decrypt);
        serverExecutor.submit(new Processor(decryptedQueue, responseQueue, itemService)::processMessages);
        serverExecutor.submit(new Encryptor(responseQueue, outgoingQueue, encryptionService, PASSWORD)::encrypt);
        serverExecutor.submit(new NetworkSender(outgoingQueue, registry)::sendMessage);
    }

    @AfterEach
    void stopServer() throws InterruptedException {
        if (serverExecutor != null && !serverExecutor.isShutdown()) {
            serverExecutor.shutdownNow();
            serverExecutor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void testTCP() throws Exception {
        StoreClientTCP client = new StoreClientTCP(SERVER_IP, TCP_PORT, encryptionService, PASSWORD);
        
        // add 50 of item 1
        Message request = new Message((byte) 1, 1001L, 3, 1, "{\"id\":1, \"amount\":50}");
        Message response = client.sendRequest(request, 3);
        
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.commandType()).isZero();
        Assertions.assertThat(itemRepository.findById(1).getQuantity()).isEqualTo(150);
        
        client.closeConnection();
    }

    @Test
    void testUDP() throws Exception {
        StoreClientUDP client = new StoreClientUDP(SERVER_IP, UDP_PORT, encryptionService, PASSWORD);
        
        // reduce 20 of item 2
        Message request = new Message((byte) 2, 2001L, 2, 2, "{\"id\":2, \"amount\":20}");
        Message response = client.sendRequest(request, 3);
        
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.commandType()).isZero();
        Assertions.assertThat(itemRepository.findById(2).getQuantity()).isEqualTo(480);
    }

    @Test
    void testTcpClientThrowsExceptionWhenServerIsDown() throws InterruptedException {
        StoreClientTCP client = new StoreClientTCP(SERVER_IP, TCP_PORT, encryptionService, PASSWORD);
        stopServer();
        Message request = new Message((byte) 1, 3001L, 1, 1, "1");

        Assertions.assertThatThrownBy(() -> client.sendRequest(request, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Server is unavailable after 2 attempts");
    }

    @Test
    void testUdpClientRetryAndTimeoutLogic() throws InterruptedException {
        StoreClientUDP client = new StoreClientUDP(SERVER_IP, UDP_PORT, encryptionService, PASSWORD);
        stopServer();
        Message request = new Message((byte) 2, 4001L, 1, 2, "2");

        long startTime = System.currentTimeMillis();
        Assertions.assertThatThrownBy(() -> client.sendRequest(request, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("UDP Server did not respond after 3 attempts");
        
        long duration = System.currentTimeMillis() - startTime;
        Assertions.assertThat(duration).isGreaterThanOrEqualTo(6000);
    }

    @Test
    void testMultipleClientsConcurrentAccessToOneServer() throws InterruptedException {
        int numberOfClients = 5;
        int requestsPerClient = 10;

        ExecutorService clientExecutor = Executors.newFixedThreadPool(numberOfClients * 2);
        CountDownLatch latch = new CountDownLatch(numberOfClients * 2);

        AtomicLong messageId = new AtomicLong(0);

        for (int i = 0; i < numberOfClients; i++) {
            final int clientAppNumber = i + 1;
            clientExecutor.submit(() -> {
                try {
                    StoreClientTCP tcpClient = new StoreClientTCP(SERVER_IP, TCP_PORT, encryptionService, PASSWORD);
                    for (int j = 0; j < requestsPerClient; j++) {
                        Message request = new Message((byte) clientAppNumber, messageId.getAndIncrement(), 3, 1, "{\"id\":1, \"amount\":10}");
                        tcpClient.sendRequest(request, 3);
                    }
                    tcpClient.closeConnection();
                } catch (Exception e) {
                    System.err.println("TCP Client failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < numberOfClients; i++) {
            final int clientAppNumber = i + 10;
            clientExecutor.submit(() -> {
                try {
                    StoreClientUDP udpClient = new StoreClientUDP(SERVER_IP, UDP_PORT, encryptionService, PASSWORD);
                    for (int j = 0; j < requestsPerClient; j++) {
                        Message request = new Message((byte) clientAppNumber, messageId.getAndIncrement(), 2, 1, "{\"id\":1, \"amount\":5}");
                        udpClient.sendRequest(request, 3);
                    }
                } catch (Exception e) {
                    System.err.println("UDP Client failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completedInTime = latch.await(10, TimeUnit.SECONDS);
        Assertions.assertThat(completedInTime)
                .as("All clients should finish their requests within timeout")
                .isTrue();

        Assertions.assertThat(messageId.get()).isEqualTo(100);

        // 100 + (5 clients * 10 requests * 10) - (5 clients * 10 requests * 5) = 600 - 250 = 350
        int finalStock = itemRepository.findById(1).getQuantity();
        System.out.println("Expected stock: 350, Actual stock: " + finalStock);
        Assertions.assertThat(finalStock).isEqualTo(350);
        clientExecutor.shutdownNow();
    }
}