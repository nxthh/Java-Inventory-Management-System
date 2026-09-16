package com.inventory.service;

import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidCartOperationException;
import com.inventory.model.Cart;
import com.inventory.model.CartItem;
import com.inventory.model.CheckoutTotals;
import com.inventory.model.Discount;
import com.inventory.model.Product;
import com.inventory.model.Transaction;
import com.inventory.model.payment.Payment;

import java.util.List;

/**
 * CheckoutService contains the BUSINESS RULES for turning a filled Cart
 * into a completed sale: validating it, calculating money, taking
 * payment, and - only once payment succeeds - deducting stock.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES / LAYERED ARCHITECTURE.
 * Just like ProductService and InventoryService, this class knows nothing
 * about JavaFX or text files. POSController calls it; it calls
 * ProductService (to double-check current stock) and InventoryService (to
 * deduct stock), the same way every other service in this project is used.
 *
 * OOP concept: POLYMORPHISM.
 * checkout() accepts a plain "Payment" parameter. It calls
 * payment.processPayment() without needing to know (or care) whether that
 * Payment is really a CashPayment, CardPayment, or QRPayment - each one
 * handles that call in its own way.
 */
public class CheckoutService {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final TaxCalculator taxCalculator;

    public CheckoutService(ProductService productService, InventoryService inventoryService,
                            TaxCalculator taxCalculator) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.taxCalculator = taxCalculator;
    }

    /**
     * Calculates the subtotal, discount, tax, and final total for a cart,
     * WITHOUT touching stock or processing any payment. This is what the
     * POS screen calls to show a live preview as the cashier types a
     * discount or picks a payment method.
     *
     * finalTotal = subtotal - discount + tax
     */
    public CheckoutTotals calculateTotals(Cart cart, Discount discount) {
        double subtotal = cart.getSubtotal();
        double discountAmount = (discount == null) ? 0 : discount.calculateDiscount(subtotal);
        double taxableAmount = subtotal - discountAmount;
        double taxAmount = taxCalculator.calculateTax(taxableAmount);
        double total = taxableAmount + taxAmount;
        return new CheckoutTotals(subtotal, discountAmount, taxAmount, total);
    }

    /**
     * Runs the full checkout process for a cart:
     *   1. Validate the cart is not empty.
     *   2. Validate every item still has enough stock right now.
     *   3-6. Calculate subtotal, discount, tax, and the final total.
     *   7. Process the payment (Cash / Card / QR each decide this differently).
     *   8. ONLY if payment succeeds: deduct stock for every item.
     *   9. Build a Transaction object describing what just happened.
     *   10. Clear the cart, ready for the next customer.
     *
     * @throws InvalidCartOperationException if the cart is empty
     * @throws InsufficientStockException    if any item no longer has enough stock
     * @throws com.inventory.exception.PaymentException if the payment is invalid or fails
     */
    public Transaction checkout(Cart cart, Discount discount, Payment payment) {
        if (cart.isEmpty()) {
            throw new InvalidCartOperationException("Cannot checkout an empty cart.");
        }

        List<CartItem> items = cart.getItems();

        // Re-check stock against the CURRENT saved data, in case it
        // changed since the item was first added to the cart.
        for (CartItem item : items) {
            Product currentProduct = productService.findById(item.getProduct().getId());
            if (item.getQuantity() > currentProduct.getQuantity()) {
                throw new InsufficientStockException(
                        "Not enough stock for " + currentProduct.getName() +
                                ". Available: " + currentProduct.getQuantity() +
                                ", in cart: " + item.getQuantity());
            }
        }

        CheckoutTotals totals = calculateTotals(cart, discount);

        // Step 7: process payment. If this throws (e.g. insufficient cash),
        // execution stops right here, so stock is never touched below.
        payment.processPayment();

        // Step 8: payment succeeded - NOW, and only now, deduct stock.
        for (CartItem item : items) {
            inventoryService.stockOut(item.getProduct().getId(), item.getQuantity());
        }

        // Step 9: build a record of the completed sale. It is returned to
        // the caller but not saved to a file yet (a later phase).
        Transaction transaction = new Transaction(items, totals.getSubtotal(),
                totals.getDiscountAmount(), totals.getTaxAmount(), totals.getTotal(), payment);

        // Step 10: start fresh for the next sale.
        cart.clear();

        return transaction;
    }
}
