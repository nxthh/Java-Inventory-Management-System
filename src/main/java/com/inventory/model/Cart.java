package com.inventory.model;

import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidCartOperationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Cart represents the shopping cart for ONE customer transaction at the
 * POS (Point of Sale) screen: a list of CartItems, plus the logic to add,
 * change, remove, and total them up.
 *
 * OOP concept: AGGREGATION / COLLECTIONS.
 * A Cart *has a* List of CartItems. It owns that list and is the ONLY
 * class allowed to add/remove items from it - everything else (like
 * POSController) must go through Cart's methods (addItem, removeItem,
 * etc.) instead of touching the list directly. This is the same
 * "separation of responsibilities" idea used by ProductFileRepository:
 * calculations and rules about the cart live in exactly one place.
 *
 * IMPORTANT (Part 4 scope): Cart only tracks what the customer intends to
 * buy. It never changes a Product's real stock quantity - that only
 * happens later, during checkout (a future phase). Cart DOES check that
 * the customer never requests more than the product's current stock.
 */
public class Cart {

    private final List<CartItem> items = new ArrayList<>();

    /**
     * Adds a product to the cart with the given quantity.
     * If the product is already in the cart, the new quantity is ADDED to
     * the existing quantity instead of creating a second line for the same
     * product (so "add Coca Cola x2" then "add Coca Cola x1" becomes one
     * line of Coca Cola x3).
     *
     * @throws InvalidCartOperationException if product is null or quantity <= 0
     * @throws InsufficientStockException    if the total requested quantity
     *                                        would exceed the product's available stock
     */
    public void addItem(Product product, int quantity) {
        if (product == null) {
            throw new InvalidCartOperationException("No product selected.");
        }
        if (quantity <= 0) {
            throw new InvalidCartOperationException("Quantity must be greater than zero.");
        }

        CartItem existingItem = findItemByProductId(product.getId());

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            ensureStockIsAvailable(product, newQuantity);
            existingItem.setQuantity(newQuantity);
        } else {
            ensureStockIsAvailable(product, quantity);
            items.add(new CartItem(product, quantity));
        }
    }

    /**
     * Removes the entire line for the given product from the cart.
     *
     * @throws InvalidCartOperationException if that product is not in the cart
     */
    public void removeItem(String productId) {
        CartItem item = findItemByProductId(productId);
        if (item == null) {
            throw new InvalidCartOperationException("That product is not in the cart.");
        }
        items.remove(item);
    }

    /**
     * Changes the quantity of a product already in the cart to an exact
     * new value (not added to the old value - this replaces it).
     *
     * @throws InvalidCartOperationException if that product is not in the cart,
     *                                        or the new quantity is not positive
     * @throws InsufficientStockException    if the new quantity exceeds available stock
     */
    public void updateQuantity(String productId, int newQuantity) {
        CartItem item = findItemByProductId(productId);
        if (item == null) {
            throw new InvalidCartOperationException("That product is not in the cart.");
        }
        if (newQuantity <= 0) {
            throw new InvalidCartOperationException("Quantity must be greater than zero.");
        }
        ensureStockIsAvailable(item.getProduct(), newQuantity);
        item.setQuantity(newQuantity);
    }

    /**
     * Empties the cart completely (e.g. the cashier starts a new sale).
     */
    public void clear() {
        items.clear();
    }

    /**
     * Returns a COPY of the cart's items, so outside code (like the
     * controller) can look at what is in the cart, but cannot sneak in an
     * item without going through addItem().
     */
    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Adds up the subtotal of every line in the cart to get the cart's
     * total (product price x quantity, summed across all items).
     */
    public double getSubtotal() {
        double subtotal = 0;
        for (CartItem item : items) {
            subtotal += item.getSubtotal();
        }
        return subtotal;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Makes sure the requested total quantity for a product does not go
     * over how much stock is actually available.
     */
    private void ensureStockIsAvailable(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getQuantity()) {
            throw new InsufficientStockException(
                    "Not enough stock for " + product.getName() +
                            ". Available: " + product.getQuantity() + ", requested: " + requestedQuantity);
        }
    }

    /**
     * Looks through the cart's items to find one for the given product ID.
     * Returns null if that product is not currently in the cart (there is
     * no matching "Optional" version here, since Cart is a simple
     * in-memory helper, not a file-backed repository).
     */
    private CartItem findItemByProductId(String productId) {
        for (CartItem item : items) {
            if (item.getProduct().getId().equalsIgnoreCase(productId)) {
                return item;
            }
        }
        return null;
    }
}
