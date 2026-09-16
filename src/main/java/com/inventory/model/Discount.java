package com.inventory.model;

/**
 * Discount describes ANY way of taking money off a sale.
 *
 * OOP concept: INTERFACE / ABSTRACTION.
 * This interface does not say HOW a discount is calculated - only that
 * whatever implements it must be able to answer one question: "given this
 * amount, how much money should be discounted?". PercentageDiscount is
 * the first implementation, but the checkout code below never needs to
 * know that detail. It just calls calculateDiscount(amount) and trusts
 * whichever Discount it was given to do the right thing. If a second kind
 * of discount is added later (e.g. a flat "$5 off" discount), the checkout
 * code would not need to change at all.
 */
public interface Discount {

    /**
     * Calculates how much money should be discounted from the given
     * amount. Implementations should return a value between 0 and
     * "amount" (never negative, never more than the amount itself).
     */
    double calculateDiscount(double amount);
}
