package com.inventory.repository;

import com.inventory.model.Category;
import com.inventory.model.Product;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ProductFileRepository is responsible for ALL reading and writing of
 * product data to and from the text file data/products.txt.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES.
 * File I/O (opening files, reading lines, writing lines) is messy and can
 * fail in many ways (missing file, bad permissions, etc.). By keeping all
 * of that logic in one class, the rest of the application (services,
 * controllers) never needs to know HOW the data is stored. If we later
 * switched to storing products in a database instead of a text file, only
 * this class would need to change.
 *
 * OOP concept: COLLECTIONS.
 * Products are kept and returned as an ArrayList<Product>, Java's
 * resizable array collection.
 */
public class ProductFileRepository {

    private static final String FILE_PATH = "data/products.txt";

    /**
     * Creates the repository and makes sure data/products.txt exists.
     * If the file (or the data folder) is missing, it is created and
     * filled with sample starter products so the application always has
     * something to show.
     */
    public ProductFileRepository() {
        createFileWithSampleDataIfMissing();
    }

    /**
     * Reads every line from data/products.txt and converts each line into
     * a Product object.
     *
     * @return an ArrayList containing every product currently saved
     */
    public List<Product> loadAll() {
        List<Product> products = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // skip blank lines
                }
                products.add(Product.fromFileLine(line));
            }
        } catch (IOException e) {
            System.out.println("Could not read product file: " + e.getMessage());
        }

        return products;
    }

    /**
     * Writes the given list of products to data/products.txt, replacing
     * whatever was there before. Every add/update/delete operation ends by
     * calling this method so changes are saved immediately.
     */
    public void saveAll(List<Product> products) {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            for (Product product : products) {
                writer.write(product.toFileLine());
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            System.out.println("Could not save product file: " + e.getMessage());
        }
    }

    /**
     * Adds a new product and immediately saves the updated list to disk.
     */
    public void add(Product product) {
        List<Product> products = loadAll();
        products.add(product);
        saveAll(products);
    }

    /**
     * Replaces an existing product (matched by ID) with updated data and
     * saves the change to disk.
     */
    public void update(Product updatedProduct) {
        List<Product> products = loadAll();
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId().equalsIgnoreCase(updatedProduct.getId())) {
                products.set(i, updatedProduct);
                break;
            }
        }
        saveAll(products);
    }

    /**
     * Removes the product with the given ID and saves the change to disk.
     */
    public void delete(String productId) {
        List<Product> products = loadAll();
        products.removeIf(product -> product.getId().equalsIgnoreCase(productId));
        saveAll(products);
    }

    /**
     * Searches the saved products for one matching the given ID.
     *
     * OOP concept: Optional is used instead of returning null. It clearly
     * tells the caller "this product might not exist" and forces them to
     * handle that case instead of accidentally causing a NullPointerException.
     */
    public Optional<Product> findById(String productId) {
        for (Product product : loadAll()) {
            if (product.getId().equalsIgnoreCase(productId)) {
                return Optional.of(product);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates the data folder and products.txt file with sample starter
     * products if they do not already exist. This runs once, the first
     * time the application starts.
     */
    private void createFileWithSampleDataIfMissing() {
        try {
            Path filePath = Path.of(FILE_PATH);

            if (Files.exists(filePath)) {
                return; // file already exists, nothing to do
            }

            // Make sure the "data" folder exists before creating the file inside it.
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }

            List<Product> sampleProducts = new ArrayList<>();
            sampleProducts.add(new Product("P001", "Coca Cola", Category.DRINK, 1.50, 50, 10));
            sampleProducts.add(new Product("P002", "Pepsi", Category.DRINK, 1.50, 30, 10));
            sampleProducts.add(new Product("P003", "Bread", Category.FOOD, 2.00, 15, 5));
            sampleProducts.add(new Product("P004", "Milk", Category.DAIRY, 2.50, 20, 5));
            sampleProducts.add(new Product("P005", "Instant Noodles", Category.FOOD, 1.25, 40, 10));

            saveAll(sampleProducts);
        } catch (IOException e) {
            System.out.println("Could not create product file: " + e.getMessage());
        }
    }
}
