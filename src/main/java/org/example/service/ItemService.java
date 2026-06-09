package org.example.service;

import org.example.model.Item;
import org.example.dto.ItemFilter;
import java.util.List;

public interface ItemService {
    Item getItem(int id);
    void reduceStock(int id, int amount);
    void addStock(int id, int amount);
    int createItem(Item item);
    void updateItem(Item item);
    void deleteItem(int id);
    List<Item> searchItems(ItemFilter filter);
}