package com.inventory.model;

/**
 * CheckoutTotals is a small, read-only holder for the numbers shown on
 * the checkout panel: Subtotal, Discount, Tax, and the final Total.
 *
 * OOP concept: ENCAPSULATION.
 * Every field is private and final (set once, in the constructor, and
 * never changed afterwards). Outside code can only READ these numbers
 * through the getters below - it cannot alter them. This keeps a
 * CheckoutTotals object trustworthy: once it has been calculated, nothing
 * else can quietly change the numbers being shown to the cashier.
 *
 * This class holds no logic of its own - the actual math (subtotal -
 * discount + tax) lives in CheckoutService, which is the one place that
 * builds CheckoutTotals objects.
 */
public class CheckoutTotals {

    private final double subtotal;
    private final double discountAmount;
    private final double taxAmount;
    private final double total;

    public CheckoutTotals(double subtotal, double discountAmount, double taxAmount, double total) {
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.total = total;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getTotal() {
        return total;
    }
}
