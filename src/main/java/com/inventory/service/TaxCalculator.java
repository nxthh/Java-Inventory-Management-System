package com.inventory.service;

/**
 * TaxCalculator applies a single, configurable tax rate to a taxable
 * amount.
 *
 * Example: taxRate = 0.10 (10%), taxable amount = $90 -> tax = $9.
 *
 * WHY THIS CLASS EXISTS: without it, "0.10" (or worse, "* 1.10") would
 * end up typed directly inside a controller or service method. If the
 * store's tax rate ever changed, or a second screen also needed to
 * calculate tax, every one of those scattered numbers would need to be
 * found and fixed. Instead, the tax rate is configured ONCE here (see the
 * constructor call in POSController), and every calculation goes through
 * calculateTax().
 */
public class TaxCalculator {

    private final double taxRate;

    /**
     * @param taxRate the tax rate as a decimal, e.g. 0.10 means 10%.
     */
    public TaxCalculator(double taxRate) {
        if (taxRate < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative.");
        }
        this.taxRate = taxRate;
    }

    public double getTaxRate() {
        return taxRate;
    }

    /**
     * Calculates the tax owed on a taxable amount (the subtotal AFTER the
     * discount has already been subtracted).
     */
    public double calculateTax(double taxableAmount) {
        return taxableAmount * taxRate;
    }
}
