package org.example.network.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.service.AuthService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

public class LoginHandler implements HttpHandler {
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            LoginRequest request = objectMapper.readValue(exchange.getRequestBody(), LoginRequest.class);
            String token = authService.login(request.username(), request.password());
            
            String responseJson = objectMapper.writeValueAsString(new LoginResponse(token));
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseJson.getBytes());
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 401, "Invalid credentials");
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorJson = "{\"error\": \"" + message + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorJson.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorJson.getBytes());
        }
    }
}