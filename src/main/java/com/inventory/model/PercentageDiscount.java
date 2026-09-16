package com.inventory.model;

import com.inventory.exception.InvalidDiscountException;

/**
 * PercentageDiscount takes a percentage off an amount.
 *
 * Example: amount = $100, percentage = 10 -> discount value = $10.
 *
 * OOP concept: IMPLEMENTING AN INTERFACE.
 * PercentageDiscount "implements" Discount, which means it PROMISES to
 * provide a real calculateDiscount(amount) method. This is what lets the
 * checkout code hold a plain "Discount" reference and call
 * calculateDiscount() without caring that, underneath, it is really a
 * PercentageDiscount doing percentage math.
 */
public class PercentageDiscount implements Discount {

    private final double percentage;

    /**
     * @param percentage a whole-number-style percentage, e.g. 10 means 10%.
     * @throws InvalidDiscountException if percentage is not between 0 and 100
     */
    public PercentageDiscount(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new InvalidDiscountException("Discount percentage must be between 0 and 100.");
        }
        this.percentage = percentage;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    public double calculateDiscount(double amount) {
        return amount * (percentage / 100.0);
    }
}
