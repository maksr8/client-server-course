package org.example.network.http;

import com.sun.net.httpserver.HttpServer;
import org.example.service.AuthService;
import org.example.service.ItemService;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

public class StoreHttpServer implements AutoCloseable {
    private final HttpServer server;

    public StoreHttpServer(int port, ExecutorService executor, AuthService authService, ItemService itemService,
                           JwtAuthenticator authenticator) throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);
        server.createContext("/login", new LoginHandler(authService));
        var productContext = server.createContext("/products", new ProductHandler(itemService));
        productContext.setAuthenticator(authenticator);
    }

    public void start() {
        server.start();
        System.out.println("HTTP Server is listening on port " + server.getAddress().getPort());
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(2);
            System.out.println("HTTP Server stopped.");
        }
    }
}