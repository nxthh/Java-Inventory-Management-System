package com.inventory.model.payment;

/**
 * CardPayment models paying by debit/credit card. This is SIMULATED - no
 * real bank or payment gateway is contacted. Selecting "Card" and
 * checking out is treated as an automatic success, since building a real
 * card processor is outside the scope of this project.
 *
 * OOP concept: INHERITANCE + POLYMORPHISM.
 * Just like CashPayment, CardPayment "extends" Payment. It fills in
 * processPayment() with completely different (and much simpler) logic
 * than CashPayment, but code that only knows about "a Payment" can use
 * either one the exact same way.
 */
public class CardPayment extends Payment {

    public CardPayment(double amountDue) {
        super(amountDue);
    }

    @Override
    public void processPayment() {
        // Simulated card payment: no bank connection, so it always succeeds.
        // There is no "change" for a card, so it stays at its default of 0.
        markSuccessful();
    }

    @Override
    public String getMethodName() {
        return "Card";
    }
}
