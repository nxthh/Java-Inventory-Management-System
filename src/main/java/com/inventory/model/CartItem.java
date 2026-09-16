package com.inventory.model;

/**
 * CartItem represents ONE line in a shopping cart: a Product the cashier
 * has picked, plus how many of it the customer wants to buy.
 *
 * OOP concept: COMPOSITION ("has-a" relationship).
 * A CartItem *has a* Product - it does not extend/inherit from Product,
 * it simply holds a reference to one. This models real life: a line on a
 * receipt is not itself a product, it just refers to one and says how many
 * were bought.
 *
 * OOP concept: ENCAPSULATION.
 * The product field is final (it can never be swapped for a different
 * product once the CartItem is created) and quantity is private, only
 * changeable through setQuantity(). This keeps a CartItem's data safe from
 * being corrupted by outside code, while still allowing Cart to update the
 * quantity when the cashier changes their mind.
 */
public class CartItem {

    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Calculates how much this single line of the cart costs:
     * product price x quantity.
     */
    public double getSubtotal() {
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return "CartItem{product=" + product.getName() + ", quantity=" + quantity +
                ", subtotal=" + getSubtotal() + "}";
    }
}
