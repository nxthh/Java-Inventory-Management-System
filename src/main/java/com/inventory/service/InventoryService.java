package com.inventory.service;

import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidProductException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.model.Product;
import com.inventory.repository.ProductFileRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * InventoryService contains the business rules for managing STOCK LEVELS
 * of products: adding stock, removing stock, and detecting low stock.
 *
 * This is kept separate from ProductService, which manages the product's
 * general details (name, price, category). InventoryService focuses only
 * on quantity-related operations. This is the OOP idea of giving each
 * class a single, clear responsibility.
 */
public class InventoryService {

    private final ProductFileRepository productFileRepository;

    public InventoryService(ProductFileRepository productFileRepository) {
        this.productFileRepository = productFileRepository;
    }

    /**
     * Increases a product's quantity by the given amount (e.g. new stock
     * arrives from a supplier) and saves the change.
     *
     * @throws InvalidProductException if amount is not positive
     */
    public void stockIn(String productId, int amount) {
        if (amount <= 0) {
            throw new InvalidProductException("Stock-in amount must be greater than zero.");
        }

        Product product = findProductOrThrow(productId);
        product.setQuantity(product.getQuantity() + amount);
        productFileRepository.update(product);
    }

    /**
     * Decreases a product's quantity by the given amount (e.g. a sale or
     * damaged stock) and saves the change.
     *
     * @throws InvalidProductException      if amount is not positive
     * @throws InsufficientStockException   if there is not enough stock available
     */
    public void stockOut(String productId, int amount) {
        if (amount <= 0) {
            throw new InvalidProductException("Stock-out amount must be greater than zero.");
        }

        Product product = findProductOrThrow(productId);

        if (amount > product.getQuantity()) {
            throw new InsufficientStockException(
                    "Not enough stock for " + product.getName() +
                            ". Available: " + product.getQuantity() + ", requested: " + amount);
        }

        product.setQuantity(product.getQuantity() - amount);
        productFileRepository.update(product);
    }

    /**
     * A product is considered low stock when its quantity has dropped to,
     * or below, its minimum stock level.
     */
    public boolean isLowStock(Product product) {
        return product.getQuantity() <= product.getMinimumStock();
    }

    /**
     * Returns every product that is currently low on stock, so the UI can
     * warn the user (e.g. "Reorder Bread - only 3 left").
     */
    public List<Product> getLowStockProducts() {
        List<Product> lowStockProducts = new ArrayList<>();

        for (Product product : productFileRepository.loadAll()) {
            if (isLowStock(product)) {
                lowStockProducts.add(product);
            }
        }
        return lowStockProducts;
    }

    /**
     * Looks up a product by ID, or throws ProductNotFoundException if it
     * does not exist. Shared helper used by stockIn() and stockOut().
     */
    private Product findProductOrThrow(String productId) {
        return productFileRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "No product found with ID: " + productId));
    }
}
