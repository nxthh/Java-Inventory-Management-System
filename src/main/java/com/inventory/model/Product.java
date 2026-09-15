package com.inventory.model;

/**
 * Product represents a single item sold in the store (e.g. a can of soda).
 *
 * OOP concept: ENCAPSULATION.
 * All fields are private, meaning no other class can reach in and change
 * them directly. Instead, other classes must use the public getter and
 * setter methods below. This lets Product control and protect its own
 * data (for example, we could stop a negative price from ever being set).
 */
public class Product {

    private String id;
    private String name;
    private Category category;
    private double price;
    private int quantity;
    private int minimumStock;

    /**
     * OOP concept: CONSTRUCTOR.
     * A constructor is a special method used to create a new Product object
     * with all of its required fields already filled in, instead of leaving
     * it half-empty.
     */
    public Product(String id, String name, Category category, double price, int quantity, int minimumStock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
    }

    // ----- Getters -----

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    // ----- Setters (allow controlled changes to a product) -----

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    /**
     * Converts this Product into one line of text for saving to
     * data/products.txt, using commas to separate the fields.
     * Example: P001,Coca Cola,Drink,1.50,50,10
     */
    public String toFileLine() {
        return id + "," + name + "," + category.toDisplayString() + "," + price + "," + quantity + "," + minimumStock;
    }

    /**
     * Parses one line of text from data/products.txt back into a Product
     * object. This is the reverse of toFileLine().
     */
    public static Product fromFileLine(String line) {
        String[] parts = line.split(",");
        String id = parts[0].trim();
        String name = parts[1].trim();
        Category category = Category.fromString(parts[2].trim());
        double price = Double.parseDouble(parts[3].trim());
        int quantity = Integer.parseInt(parts[4].trim());
        int minimumStock = Integer.parseInt(parts[5].trim());
        return new Product(id, name, category, price, quantity, minimumStock);
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name=" + name + ", category=" + category +
                ", price=" + price + ", quantity=" + quantity + ", minimumStock=" + minimumStock + "}";
    }
}
