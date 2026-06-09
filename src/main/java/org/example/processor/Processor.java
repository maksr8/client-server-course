package org.example.processor;

import org.example.dto.ItemFilter;
import org.example.dto.Message;
import org.example.model.Item;
import org.example.service.ItemService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class Processor {
    private final BlockingQueue<Message> queueToProcess;
    private final BlockingQueue<Message> queueToEncrypt;
    private final ItemService itemService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Processor(BlockingQueue<Message> queueToProcess, BlockingQueue<Message> queueToEncrypt, ItemService itemService) {
        this.queueToProcess = queueToProcess;
        this.queueToEncrypt = queueToEncrypt;
        this.itemService = itemService;
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
        String payload = message.messageString();
        String responseText;

        try {
            responseText = switch (command) {
                case 1 -> handleGetItem(payload);
                case 2 -> handleReduceStock(payload);
                case 3 -> handleAddStock(payload);
                case 4 -> handleCreateItem(payload);
                case 5 -> handleUpdateItem(payload);
                case 6 -> handleDeleteItem(payload);
                case 7 -> handleSearchItems(payload);
                default -> "Error: Unknown command " + command;
            };
        } catch (Exception e) {
            responseText = "Error processing command [" + command + "]: " + e.getMessage();
        }

        return new Message(message.clientAppNumber(), message.messageID(), 0, message.userID(), responseText);
    }

    private String handleGetItem(String payload) throws Exception {
        int id = payload.trim().startsWith("{")
                ? objectMapper.readTree(payload).get("id").asInt()
                : Integer.parseInt(payload.trim());

        Item item = itemService.getItem(id);
        return objectMapper.writeValueAsString(item);
    }

    private String handleReduceStock(String payload) throws Exception {
        var jsonNode = objectMapper.readTree(payload);
        int id = jsonNode.get("id").asInt();
        int amount = jsonNode.get("amount").asInt();
        itemService.reduceStock(id, amount);
        return "Reduced " + amount + " quantity for item with ID: " + id;
    }

    private String handleAddStock(String payload) throws Exception {
        var jsonNode = objectMapper.readTree(payload);
        int id = jsonNode.get("id").asInt();
        int amount = jsonNode.get("amount").asInt();
        itemService.addStock(id, amount);
        return "Added " + amount + " quantity for item with ID: " + id;
    }

    private String handleCreateItem(String payload) throws Exception {
        Item newItem = objectMapper.readValue(payload, Item.class);
        int generatedId = itemService.createItem(newItem);
        return "Item created successfully with ID: " + generatedId;
    }

    private String handleUpdateItem(String payload) throws Exception {
        Item updateData = objectMapper.readValue(payload, Item.class);
        itemService.updateItem(updateData);
        return "Item updated successfully for ID: " + updateData.getId();
    }

    private String handleDeleteItem(String payload) throws Exception {
        int id = payload.trim().startsWith("{")
                ? objectMapper.readTree(payload).get("id").asInt()
                : Integer.parseInt(payload.trim());

        itemService.deleteItem(id);
        return "Item deleted successfully for ID: " + id;
    }

    private String handleSearchItems(String payload) throws Exception {
        ItemFilter filter = objectMapper.readValue(payload, ItemFilter.class);
        List<Item> results = itemService.searchItems(filter);
        return objectMapper.writeValueAsString(results);
    }
}