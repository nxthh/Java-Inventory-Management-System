package com.inventory.model.payment;

/**
 * Payment represents ONE attempt to pay for a sale. It is the shared
 * parent of CashPayment, CardPayment, and QRPayment.
 *
 * OOP concept: ABSTRACTION + INHERITANCE.
 * Payment is declared "abstract", which means you can never create a
 * plain "new Payment(...)" directly - it only exists to be extended by a
 * real payment type. Every subclass "is-a" Payment and must supply its
 * own processPayment() and getMethodName(), while sharing the amountDue
 * and change fields defined here. This avoids repeating the same fields
 * and getters in every payment class.
 *
 * OOP concept: POLYMORPHISM.
 * Code elsewhere (like CheckoutService) can hold a variable of type
 * Payment and call payment.processPayment() without caring whether the
 * actual object underneath is a CashPayment, CardPayment, or QRPayment.
 * Each subclass processes the payment in its own way, but the calling
 * code stays exactly the same either way.
 *
 * OOP concept: ENCAPSULATION.
 * amountDue and change are "protected", meaning only Payment and its
 * subclasses can touch them directly. Every other class (like a
 * controller) must go through the public getters below.
 */
public abstract class Payment {

    protected final double amountDue;
    protected double change;
    private boolean successful;

    protected Payment(double amountDue) {
        this.amountDue = amountDue;
    }

    /**
     * Attempts to process this payment. Each payment type decides its own
     * rules for what "success" means (see CashPayment, CardPayment,
     * QRPayment). If the payment cannot go through, this method throws
     * com.inventory.exception.PaymentException instead of returning a
     * value, so the caller cannot accidentally forget to check a boolean.
     */
    public abstract void processPayment();

    /**
     * A short display name for this payment type, e.g. "Cash", "Card", "QR".
     */
    public abstract String getMethodName();

    public double getAmountDue() {
        return amountDue;
    }

    public double getChange() {
        return change;
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Subclasses call this once their own payment rules have passed, so
     * that isSuccessful() correctly reports true afterwards.
     */
    protected void markSuccessful() {
        this.successful = true;
    }
}
