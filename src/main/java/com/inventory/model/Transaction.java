package com.inventory.model;

import com.inventory.model.payment.Payment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transaction is a record of ONE completed sale: which items were bought,
 * the calculated subtotal/discount/tax/total, and how the customer paid.
 *
 * IMPORTANT (Part 5 scope): a Transaction object is created at the end of
 * a successful checkout, but it is NOT saved to a file yet. Persisting
 * transaction history (and building reports from it) is explicitly a
 * later phase. For now this class simply exists so checkout has
 * something to hand back describing what just happened (used to build
 * the "Checkout Complete" summary shown to the cashier).
 *
 * OOP concept: COMPOSITION.
 * A Transaction "has a" List of CartItems and "has a" Payment - it is
 * built FROM other objects rather than inheriting from them, the same
 * way Cart is composed of CartItems.
 */
public class Transaction {

    private final List<CartItem> items;
    private final double subtotal;
    private final double discountAmount;
    private final double taxAmount;
    private final double total;
    private final Payment payment;
    private final LocalDateTime dateTime;

    public Transaction(List<CartItem> items, double subtotal, double discountAmount,
                        double taxAmount, double total, Payment payment) {
        this.items = items;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.total = total;
        this.payment = payment;
        this.dateTime = LocalDateTime.now();
    }

    public List<CartItem> getItems() {
        return items;
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

    public Payment getPayment() {
        return payment;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}
