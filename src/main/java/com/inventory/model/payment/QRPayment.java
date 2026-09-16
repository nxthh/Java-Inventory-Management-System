package com.inventory.model.payment;

/**
 * QRPayment models paying by scanning a QR code (e.g. a mobile banking
 * app). This is SIMULATED - no real QR/payment gateway is contacted.
 * Selecting "QR" and checking out is treated as an automatic success.
 *
 * OOP concept: INHERITANCE + POLYMORPHISM.
 * A third subclass of Payment, alongside CashPayment and CardPayment.
 * The checkout code in CheckoutService does not need a third code path
 * for this - it already works with any Payment, including this one.
 */
public class QRPayment extends Payment {

    public QRPayment(double amountDue) {
        super(amountDue);
    }

    @Override
    public void processPayment() {
        // Simulated QR payment: no gateway connection, so it always succeeds.
        markSuccessful();
    }

    @Override
    public String getMethodName() {
        return "QR";
    }
}
