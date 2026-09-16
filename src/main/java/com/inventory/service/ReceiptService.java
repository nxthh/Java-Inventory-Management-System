package com.inventory.service;

import com.inventory.model.CartItem;
import com.inventory.model.Transaction;
import com.inventory.repository.ReceiptFileRepository;

import java.time.format.DateTimeFormatter;

/**
 * ReceiptService turns a completed Transaction into a human-readable
 * receipt (plain text), and is the only class that knows HOW a receipt
 * should be laid out.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES / LAYERED ARCHITECTURE.
 * Just like CheckoutService, this class knows nothing about JavaFX or
 * where files live on disk - it builds text, then hands saving/loading
 * off to ReceiptFileRepository. Controllers never format or save a
 * receipt themselves; they call this service.
 */
public class ReceiptService {

    // The store name printed at the top of every receipt. Kept as ONE
    // configurable value here, the same way TaxCalculator keeps the tax
    // rate in one place, instead of this text being repeated everywhere.
    private static final String STORE_NAME = "MY STORE";
    private static final int LINE_WIDTH = 32;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReceiptFileRepository receiptFileRepository;

    public ReceiptService(ReceiptFileRepository receiptFileRepository) {
        this.receiptFileRepository = receiptFileRepository;
    }

    /**
     * Asks the repository for the next unused receipt ID (e.g. "R0001").
     * Kept as its own method so CheckoutService can generate the ID
     * BEFORE the Transaction object is built (a Transaction needs to
     * know its own receiptId so it can be saved together in one line -
     * see Transaction.toFileLines()).
     */
    public String generateReceiptId() {
        return receiptFileRepository.generateNextReceiptId();
    }

    /**
     * Builds the full receipt text for a completed transaction and saves
     * it to data/receipts/{transaction.getReceiptId()}.txt.
     *
     * @throws com.inventory.exception.ReceiptException if the transaction
     *         has no receiptId, or the file could not be written
     */
    public void generateAndSaveReceipt(Transaction transaction) {
        String content = buildReceiptText(transaction);
        receiptFileRepository.saveReceipt(transaction.getReceiptId(), content);
    }

    /**
     * Loads a previously saved receipt's text back from disk, for the
     * Transaction History screen (or immediately after checkout, so the
     * exact saved copy is what gets displayed).
     *
     * @throws com.inventory.exception.ReceiptException if the receipt ID
     *         is blank or the file cannot be found/read
     */
    public String loadReceiptText(String receiptId) {
        return receiptFileRepository.loadReceipt(receiptId);
    }

    /**
     * Builds the receipt's text layout from a Transaction's data. This
     * does NOT touch the file system - see generateAndSaveReceipt().
     */
    private String buildReceiptText(Transaction transaction) {
        StringBuilder receipt = new StringBuilder();

        String border = "=".repeat(LINE_WIDTH);
        String divider = "-".repeat(LINE_WIDTH);

        receipt.append(border).append(System.lineSeparator());
        receipt.append(centerText(STORE_NAME)).append(System.lineSeparator());
        receipt.append(border).append(System.lineSeparator());
        receipt.append(System.lineSeparator());

        receipt.append("Receipt ID: ").append(transaction.getReceiptId()).append(System.lineSeparator());
        receipt.append("Date: ").append(transaction.getDateTime().format(DATE_FORMAT)).append(System.lineSeparator());
        receipt.append("Cashier: ").append(transaction.getCashier()).append(System.lineSeparator());
        receipt.append(System.lineSeparator());

        receipt.append(divider).append(System.lineSeparator());
        receipt.append(String.format("%-14s%-8s%10s", "Product", "Qty", "Total")).append(System.lineSeparator());
        receipt.append(divider).append(System.lineSeparator());

        for (CartItem item : transaction.getItems()) {
            String name = truncate(item.getProduct().getName(), 13);
            String itemTotal = money(item.getSubtotal());
            receipt.append(String.format("%-14s%-8d%10s", name, item.getQuantity(), itemTotal))
                    .append(System.lineSeparator());
        }
        receipt.append(divider).append(System.lineSeparator());
        receipt.append(System.lineSeparator());

        receipt.append(moneyLine("Subtotal:", transaction.getSubtotal())).append(System.lineSeparator());
        receipt.append(moneyLine("Discount:", transaction.getDiscountAmount())).append(System.lineSeparator());
        receipt.append(moneyLine("Tax:", transaction.getTaxAmount())).append(System.lineSeparator());
        receipt.append(divider).append(System.lineSeparator());
        receipt.append(moneyLine("TOTAL:", transaction.getTotal())).append(System.lineSeparator());
        receipt.append(System.lineSeparator());

        receipt.append(String.format("%-18s%14s", "Payment:", transaction.getPaymentMethod()))
                .append(System.lineSeparator());
        if ("Cash".equals(transaction.getPaymentMethod())) {
            receipt.append(moneyLine("Amount Paid:", transaction.getAmountPaid())).append(System.lineSeparator());
            receipt.append(moneyLine("Change:", transaction.getChange())).append(System.lineSeparator());
        }
        receipt.append(System.lineSeparator());

        receipt.append(border).append(System.lineSeparator());
        receipt.append(centerText("Thank You!")).append(System.lineSeparator());
        receipt.append(border).append(System.lineSeparator());

        return receipt.toString();
    }

    /** Formats one "Label:   $0.00" line, right-aligned to LINE_WIDTH. */
    private String moneyLine(String label, double amount) {
        return String.format("%-18s%14s", label, money(amount));
    }

    private String money(double amount) {
        return String.format("$%.2f", amount);
    }

    /** Cuts a product name down so it never breaks the column layout. */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /** Centers a short line of text within LINE_WIDTH characters. */
    private String centerText(String text) {
        int padding = Math.max(0, (LINE_WIDTH - text.length()) / 2);
        return " ".repeat(padding) + text;
    }
}
