package org.example.network.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.model.Item;
import org.example.service.ItemService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

public class ProductHandler implements HttpHandler {
    private final ItemService itemService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductHandler(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        try {
            switch (method) {
                case "GET" -> handleGet(exchange, pathParts);
                case "PUT" -> handleUpdate(exchange, pathParts);
                case "POST" -> handleCreate(exchange);
                case "DELETE" -> handleDelete(exchange, pathParts);
                default -> sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (RuntimeException e) {
            String fullError = e.getMessage();
            if (e.getCause() != null) {
                fullError += " " + e.getCause().getMessage();
            }
            fullError = fullError.toLowerCase();

            if (fullError.contains("not found")) {
                sendResponse(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
            } else if (fullError.contains("duplicate") || fullError.contains("unique")) {
                sendResponse(exchange, 409, "{\"error\":\"Item name already exists\"}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    private void handleGet(HttpExchange exchange, String[] pathParts) throws IOException {
        if (pathParts.length < 3) {
            throw new IllegalArgumentException("Product ID is required");
        }
        int id = Integer.parseInt(pathParts[2]);
        Item item = itemService.getItem(id);
        sendResponse(exchange, 200, objectMapper.writeValueAsString(item));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        Item newItem = objectMapper.readValue(exchange.getRequestBody(), Item.class);
        int generatedId = itemService.createItem(newItem);
        sendResponse(exchange, 201, "{\"id\": " + generatedId + ", \"message\": \"Created successfully\"}");
    }

    private void handleUpdate(HttpExchange exchange, String[] pathParts) throws IOException {
        if (pathParts.length < 3) {
            throw new IllegalArgumentException("Product ID is required for update");
        }
        int id = Integer.parseInt(pathParts[2]);
        Item itemToUpdate = objectMapper.readValue(exchange.getRequestBody(), Item.class);
        itemToUpdate.setId(id);
        itemService.updateItem(itemToUpdate);
        sendResponse(exchange, 200, "{\"message\": \"Updated successfully\"}");
    }

    private void handleDelete(HttpExchange exchange, String[] pathParts) throws IOException {
        if (pathParts.length < 3) {
            throw new IllegalArgumentException("Product ID is required for deletion");
        }
        int id = Integer.parseInt(pathParts[2]);
        itemService.deleteItem(id);
        sendResponse(exchange, 200, "{\"message\": \"Deleted successfully\"}");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] response = jsonBody.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}