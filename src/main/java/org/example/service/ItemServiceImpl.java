package org.example.service;

import org.example.dto.ItemFilter;
import org.example.model.Item;
import org.example.repository.ItemRepository;

import java.util.List;

public class ItemServiceImpl implements ItemService {
    
    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public Item getItem(int id) {
        Item item = itemRepository.findById(id);
        if (item == null) {
            throw new RuntimeException("Item with ID [" + id + "] not found.");
        }
        return item;
    }

    @Override
    public void reduceStock(int id, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to reduce must be greater than zero.");
        }
        
        boolean success = itemRepository.reduceQuantity(id, amount);
        if (!success) {
            throw new RuntimeException("Failed to reduce stock for ID [" + id + "].");
        }
    }

    @Override
    public void addStock(int id, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to add must be greater than zero.");
        }
        boolean success = itemRepository.addQuantity(id, amount);
        if (!success) {
            throw new RuntimeException("Item with ID [" + id + "] not found.");
        }
    }

    @Override
    public int createItem(Item item) {
        if (item.getPrice() == null || item.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative or null.");
        }
        if (item.getQuantity() == null || item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative or null.");
        }
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }

        return itemRepository.create(item);
    }

    @Override
    public void updateItem(Item item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("Cannot update item without an ID.");
        }
        if (item.getPrice() == null || item.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative or null.");
        }
        if (item.getQuantity() == null || item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative or null.");
        }
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        
        boolean success = itemRepository.update(item);
        if (!success) {
            throw new RuntimeException("Item with ID [" + item.getId() + "] not found for update.");
        }
    }

    @Override
    public void deleteItem(int id) {
        boolean success = itemRepository.delete(id);
        if (!success) {
            throw new RuntimeException("Item with ID [" + id + "] not found.");
        }
    }

    @Override
    public List<Item> searchItems(ItemFilter filter) {
        int safeLimit = (filter.limit() > 0 && filter.limit() <= 100) ? filter.limit() : 20;
        int safeOffset = Math.max(0, filter.offset());

        ItemFilter itemFilter = new ItemFilter(
                filter.name(),
                filter.category(),
                filter.minPrice(),
                filter.maxPrice(),
                filter.minQuantity(),
                filter.maxQuantity(),
                safeLimit,
                safeOffset
        );
        return itemRepository.search(itemFilter);
    }
}