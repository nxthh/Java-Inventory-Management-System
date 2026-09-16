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
import com.inventory.repository.TransactionFileRepository;
import com.inventory.util.Session;

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
 *
 * Part 6B note: after a Transaction is created and saved, CheckoutService
 * now also asks ReceiptService to build and save a Receipt for that same
 * sale, using the same "hand it off to a service" pattern used for
 * everything else in this class.
 */
public class CheckoutService {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final TaxCalculator taxCalculator;
    private final TransactionFileRepository transactionFileRepository;
    private final ReceiptService receiptService;

    public CheckoutService(ProductService productService, InventoryService inventoryService,
                            TaxCalculator taxCalculator, TransactionFileRepository transactionFileRepository,
                            ReceiptService receiptService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.taxCalculator = taxCalculator;
        this.transactionFileRepository = transactionFileRepository;
        this.receiptService = receiptService;
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
     *   8. Build a Transaction object describing what just happened.
     *   9. Save the transaction to data/transactions.txt so it survives a restart.
     *   10. Generate and save a Receipt for this transaction.
     *   11. ONLY if payment succeeded: deduct stock for every item.
     *   12. Clear the cart, ready for the next customer.
     *
     * Note on ordering: the transaction's receiptId is decided BEFORE the
     * Transaction object is built (step 8), because Transaction saves its
     * receiptId as part of its own line in transactions.txt (see
     * Transaction.toFileLines()) - the ID has to already exist to be
     * written down. Payment is still always processed first: if it
     * fails, an exception is thrown immediately and nothing below this
     * point ever runs, so a failed payment never creates a transaction
     * or a receipt, and never touches stock.
     *
     * @throws InvalidCartOperationException if the cart is empty
     * @throws InsufficientStockException    if any item no longer has enough stock
     * @throws com.inventory.exception.PaymentException if the payment is invalid or fails
     * @throws com.inventory.exception.ReceiptException  if the receipt could not be saved
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
        // execution stops right here - no transaction, no receipt, and no
        // stock change ever happens for a failed payment.
        payment.processPayment();

        // Step 8: payment succeeded - build a record of the completed
        // sale, giving it a fresh unique transaction ID and receipt ID,
        // and remembering who was logged in as the cashier.
        String transactionId = transactionFileRepository.generateNextTransactionId();
        String receiptId = receiptService.generateReceiptId();
        String cashier = Session.getCurrentUsername();
        Transaction transaction = new Transaction(transactionId, cashier, items, totals.getSubtotal(),
                totals.getDiscountAmount(), totals.getTaxAmount(), totals.getTotal(), payment, receiptId);

        // Step 9: persist the transaction immediately, so it survives
        // an application restart even if nothing else happens after this.
        transactionFileRepository.saveTransaction(transaction);

        // Step 10: generate and save the printable receipt for this sale.
        receiptService.generateAndSaveReceipt(transaction);

        // Step 11: only now deduct stock - the sale is fully recorded.
        for (CartItem item : items) {
            inventoryService.stockOut(item.getProduct().getId(), item.getQuantity());
        }

        // Step 12: start fresh for the next sale.
        cart.clear();

        return transaction;
    }
}
