package com.inventory.service;

import com.inventory.exception.DuplicateProductException;
import com.inventory.exception.InvalidProductException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.model.Category;
import com.inventory.model.Product;
import com.inventory.repository.ProductFileRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ProductService contains the BUSINESS RULES for working with products:
 * validation, uniqueness checks, and coordinating with the repository.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES / LAYERED ARCHITECTURE.
 * The service does not know anything about text files - it only talks to
 * ProductFileRepository, which handles that. Later, a JavaFX controller
 * will call this service (never the repository directly), so the
 * controller stays free of both file handling AND business rules.
 */
public class ProductService {

    private final ProductFileRepository productFileRepository;

    public ProductService(ProductFileRepository productFileRepository) {
        this.productFileRepository = productFileRepository;
    }

    /**
     * Returns every product currently saved.
     */
    public List<Product> getAllProducts() {
        return productFileRepository.loadAll();
    }

    /**
     * Finds a single product by its ID.
     *
     * @throws ProductNotFoundException if no product with that ID exists
     */
    public Product findById(String id) {
        Optional<Product> product = productFileRepository.findById(id);
        if (product.isEmpty()) {
            throw new ProductNotFoundException("No product found with ID: " + id);
        }
        return product.get();
    }

    /**
     * Validates and adds a new product.
     *
     * @throws InvalidProductException     if any field fails validation
     * @throws DuplicateProductException   if the ID is already in use
     */
    public void addProduct(Product product) {
        validateProduct(product);

        if (productFileRepository.findById(product.getId()).isPresent()) {
            throw new DuplicateProductException("A product with ID " + product.getId() + " already exists.");
        }

        productFileRepository.add(product);
    }

    /**
     * Validates and saves changes to an existing product.
     *
     * @throws InvalidProductException  if any field fails validation
     * @throws ProductNotFoundException if the product does not exist
     */
    public void updateProduct(Product product) {
        validateProduct(product);

        // Confirms the product exists before allowing the update.
        // findById() throws ProductNotFoundException automatically if it is missing.
        findById(product.getId());

        productFileRepository.update(product);
    }

    /**
     * Deletes the product with the given ID.
     *
     * @throws ProductNotFoundException if the product does not exist
     */
    public void deleteProduct(String id) {
        // Confirms the product exists before allowing the delete.
        findById(id);

        productFileRepository.delete(id);
    }

    /**
     * Searches for products whose name contains the given text
     * (case-insensitive). Useful for a search box in the UI later.
     */
    public List<Product> searchByName(String keyword) {
        List<Product> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (Product product : getAllProducts()) {
            if (product.getName().toLowerCase().contains(lowerKeyword)) {
                results.add(product);
            }
        }
        return results;
    }

    /**
     * Returns only the products that belong to the given category.
     */
    public List<Product> filterByCategory(Category category) {
        List<Product> results = new ArrayList<>();

        for (Product product : getAllProducts()) {
            if (product.getCategory() == category) {
                results.add(product);
            }
        }
        return results;
    }

    /**
     * Checks that a product's data makes sense before it is saved.
     *
     * @throws InvalidProductException describing the first problem found
     */
    private void validateProduct(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            throw new InvalidProductException("Product ID cannot be empty.");
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new InvalidProductException("Product name cannot be empty.");
        }
        if (product.getPrice() < 0) {
            throw new InvalidProductException("Product price cannot be negative.");
        }
        if (product.getQuantity() < 0) {
            throw new InvalidProductException("Product quantity cannot be negative.");
        }
        if (product.getMinimumStock() < 0) {
            throw new InvalidProductException("Product minimum stock cannot be negative.");
        }
    }
}
