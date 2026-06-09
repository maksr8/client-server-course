package org.example;

import org.assertj.core.api.Assertions;
import org.example.dto.ItemFilter;
import org.example.model.Item;
import org.example.repository.ItemRepository;
import org.example.service.ItemService;
import org.example.service.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

class ItemServiceTest extends BasePostgresqlTest {
    private ItemService itemService;

    @BeforeEach
    void setUp() throws Exception {
        ItemRepository itemRepository = new ItemRepository(connectionProvider);
        itemService = new ItemServiceImpl(itemRepository);
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE items RESTART IDENTITY");
        }
    }

    @Test
    void testCreateItem() {
        Item newItem = new Item(null, "Laptop", "Electronics", 15000.0, 10);
        int generatedId = itemService.createItem(newItem);
        
        Assertions.assertThat(generatedId).isGreaterThan(0);
        Item savedItem = itemService.getItem(generatedId);
        Assertions.assertThat(savedItem.getName()).isEqualTo("Laptop");
    }

    @Test
    void testCreatingItemWithNegativePriceShouldThrow() {
        Item invalidItem = new Item(null, "Laptop", "Electronics", -100.0, 10);

        Assertions.assertThatThrownBy(() -> itemService.createItem(invalidItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price cannot be negative");
    }

    @Test
    void testGetItemById() {
        int id = itemService.createItem(new Item(null, "Apple", "Food", 2.5, 100));
        Item foundItem = itemService.getItem(id);

        Assertions.assertThat(foundItem).isNotNull();
        Assertions.assertThat(foundItem.getName()).isEqualTo("Apple");
    }

    @Test
    void testItemNotFoundShouldThrow() {
        Assertions.assertThatThrownBy(() -> itemService.getItem(67))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testAddStock() {
        int id = itemService.createItem(new Item(null, "Mouse", "Electronics", 2500.0, 50));
        itemService.addStock(id, 20);

        Item updatedItem = itemService.getItem(id);
        Assertions.assertThat(updatedItem.getQuantity()).isEqualTo(70);
    }

    @Test
    void testReduceStock() {
        int id = itemService.createItem(new Item(null, "Keyboard", "Electronics", 500.0, 30));
        itemService.reduceStock(id, 10);

        Item updatedItem = itemService.getItem(id);
        Assertions.assertThat(updatedItem.getQuantity()).isEqualTo(20);
    }

    @Test
    void testReducingStockBelowZeroShouldThrow() {
        int id = itemService.createItem(new Item(null, "Monitor", "Electronics", 300.0, 5));

        Assertions.assertThatThrownBy(() -> itemService.reduceStock(id, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to reduce stock");
        
        Assertions.assertThat(itemService.getItem(id).getQuantity()).isEqualTo(5);
    }

    @Test
    void testUpdateItem() {
        int id = itemService.createItem(new Item(null, "Desk", "Furniture", 150.0, 5));
        Item itemToUpdate = new Item(id, "Standing Desk", "Furniture", 250.0, 10);
        itemService.updateItem(itemToUpdate);

        Item updatedItem = itemService.getItem(id);
        Assertions.assertThat(updatedItem.getName()).isEqualTo("Standing Desk");
        Assertions.assertThat(updatedItem.getPrice()).isEqualTo(250.0);
    }

    @Test
    void testDeleteItem() {
        int id = itemService.createItem(new Item(null, "Chair", "Furniture", 75.0, 15));
        
        itemService.deleteItem(id);

        Assertions.assertThatThrownBy(() -> itemService.getItem(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testSearchItems() {
        itemService.createItem(new Item(null, "Asus Laptop", "Laptops", 10000.0, 5));
        itemService.createItem(new Item(null, "MacBook", "Laptops", 12000.0, 10));
        itemService.createItem(new Item(null, "Dell", "Laptops", 15000.0, 3));
        itemService.createItem(new Item(null, "Logitech Mouse", "Accessories", 500.0, 100));

        ItemFilter filter1 = new ItemFilter(null, "Laptops", 11000.0, 20000.0, null, null, 10, 0);
        List<Item> result1 = itemService.searchItems(filter1);
        Assertions.assertThat(result1).hasSize(2);
        Assertions.assertThat(result1).extracting(Item::getName)
                .containsExactlyInAnyOrder("MacBook", "Dell");

        ItemFilter filter2 = new ItemFilter(null, "Laptops", null, null, null, null, 1, 1);
        List<Item> result2 = itemService.searchItems(filter2);
        Assertions.assertThat(result2).hasSize(1);
        
        ItemFilter filter3 = new ItemFilter("Logitech Mouse", null, null, null, null, null, 10, 0);
        List<Item> result3 = itemService.searchItems(filter3);
        Assertions.assertThat(result3).hasSize(1);
        Assertions.assertThat(result3.getFirst().getCategory()).isEqualTo("Accessories");
    }
}