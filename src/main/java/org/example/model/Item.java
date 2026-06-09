package org.example.model;

public class Item {
    private Integer id;
    private String name;
    private String category;
    private Double price;
    private Integer quantity;

    public Item() {
    }

    public Item(Integer id, String name, String category, Double price, Integer quantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "Item{id=" + id + ", name='" + name + "', category='" + category + "', price=" + price + ", quantity=" + quantity + '}';
    }
}