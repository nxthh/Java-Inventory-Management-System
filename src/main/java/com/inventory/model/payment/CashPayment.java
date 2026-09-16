package com.inventory.model.payment;

import com.inventory.exception.PaymentException;

/**
 * CashPayment models paying with physical cash: the cashier enters how
 * much cash the customer handed over (amountPaid), and this class works
 * out the change - or rejects the payment if not enough cash was given.
 *
 * Example: Total = $9.90, Amount Paid = $20.00 -> Change = $10.10.
 *
 * OOP concept: INHERITANCE.
 * CashPayment "extends" Payment, so it automatically has amountDue and
 * change without redeclaring them, and it must fill in the two abstract
 * methods (processPayment, getMethodName) that Payment promised existed.
 */
public class CashPayment extends Payment {

    private final double amountPaid;

    public CashPayment(double amountDue, double amountPaid) {
        super(amountDue);
        if (amountPaid < 0) {
            throw new PaymentException("Amount paid cannot be negative.");
        }
        this.amountPaid = amountPaid;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    /**
     * @throws PaymentException if the amount paid is less than the total due
     */
    @Override
    public void processPayment() {
        if (amountPaid < amountDue) {
            throw new PaymentException(String.format(
                    "Insufficient cash. Total is $%.2f but only $%.2f was paid.", amountDue, amountPaid));
        }
        change = amountPaid - amountDue;
        markSuccessful();
    }

    @Override
    public String getMethodName() {
        return "Cash";
    }
}
