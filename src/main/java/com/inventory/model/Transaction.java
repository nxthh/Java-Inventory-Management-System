package com.inventory.model;

import com.inventory.model.payment.CashPayment;
import com.inventory.model.payment.Payment;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction is a record of ONE completed sale: which items were bought,
 * the calculated subtotal/discount/tax/total, and how the customer paid.
 *
 * OOP concept: COMPOSITION.
 * A Transaction "has a" List of CartItems - it is built FROM other
 * objects rather than inheriting from them, the same way Cart is
 * composed of CartItems.
 *
 * Part 6A note: a Transaction is now PERSISTED. Instead of holding on to
 * a live Payment object (CashPayment/CardPayment/QRPayment), it stores
 * the plain facts a receipt needs - paymentMethod, amountPaid, change -
 * as simple fields. This matters because when a Transaction is LOADED
 * back from data/transactions.txt after the app restarts, there is no
 * real payment "processing" happening anymore - we only need to
 * remember what already happened. Simple fields are easy to save to a
 * text file and load back, exactly like Product does with
 * toFileLine()/fromFileLine().
 */
public class Transaction {

    private final String transactionId;
    private final LocalDateTime dateTime;
    private final String cashier;
    private final List<CartItem> items;
    private final double subtotal;
    private final double discountAmount;
    private final double taxAmount;
    private final double total;
    private final String paymentMethod;
    private final double amountPaid;
    private final double change;

    /**
     * Full constructor - mainly used by TransactionFileRepository when
     * rebuilding a Transaction object from a saved file, since a saved
     * transaction already has a fixed ID and date/time.
     */
    public Transaction(String transactionId, LocalDateTime dateTime, String cashier,
                        List<CartItem> items, double subtotal, double discountAmount,
                        double taxAmount, double total, String paymentMethod,
                        double amountPaid, double change) {
        this.transactionId = transactionId;
        this.dateTime = dateTime;
        this.cashier = cashier;
        this.items = items;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.amountPaid = amountPaid;
        this.change = change;
    }

    /**
     * Convenience constructor used by CheckoutService right after a sale
     * completes: the date/time is "now", and the payment details
     * (method / amount paid / change) are pulled out of whichever
     * Payment subclass was used.
     *
     * OOP concept: POLYMORPHISM. This constructor works the same way no
     * matter which kind of Payment it is given - it only ever calls
     * methods declared on the abstract Payment class, plus one
     * "instanceof" check to read CashPayment's extra amountPaid detail.
     */
    public Transaction(String transactionId, String cashier, List<CartItem> items,
                        double subtotal, double discountAmount, double taxAmount,
                        double total, Payment payment) {
        this(transactionId, LocalDateTime.now(), cashier, items, subtotal, discountAmount,
                taxAmount, total, payment.getMethodName(),
                (payment instanceof CashPayment cashPayment) ? cashPayment.getAmountPaid() : total,
                payment.getChange());
    }

    // ----- Getters -----

    public String getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getCashier() {
        return cashier;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getTotal() {
        return total;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public double getChange() {
        return change;
    }

    // ----- File I/O helpers -----

    /**
     * Converts this Transaction into several lines of text, ready to be
     * appended to data/transactions.txt. A transaction is stored as a
     * small "block" of lines: one TRANSACTION line with the sale's
     * summary numbers, one ITEM line per purchased product, and a
     * closing END line so the reader knows where the block stops.
     *
     * Example:
     *   TRANSACTION,T0001,2025-01-10T09:15:30,ADMIN,9.90,0.00,0.90,10.80,Cash,20.00,9.20
     *   ITEM,P001,Coca Cola,Drink,1.50,2
     *   ITEM,P003,Bread,Food,2.00,1
     *   END
     */
    public List<String> toFileLines() {
        List<String> lines = new ArrayList<>();

        lines.add(String.join(",",
                "TRANSACTION",
                transactionId,
                dateTime.toString(),
                cashier,
                String.valueOf(subtotal),
                String.valueOf(discountAmount),
                String.valueOf(taxAmount),
                String.valueOf(total),
                paymentMethod,
                String.valueOf(amountPaid),
                String.valueOf(change)));

        for (CartItem item : items) {
            Product product = item.getProduct();
            lines.add(String.join(",",
                    "ITEM",
                    product.getId(),
                    product.getName(),
                    product.getCategory().toDisplayString(),
                    String.valueOf(product.getPrice()),
                    String.valueOf(item.getQuantity())));
        }

        lines.add("END");
        return lines;
    }

    /**
     * Rebuilds a Transaction object from one block of lines previously
     * produced by toFileLines() (a TRANSACTION line, zero or more ITEM
     * lines, then an END line).
     *
     * OOP concept: this mirrors Product.fromFileLine() - the class that
     * knows how to WRITE its own file format is also the class that
     * knows how to READ it back.
     *
     * @throws IllegalArgumentException if the block is missing required
     *                                   lines or any value cannot be parsed.
     *                                   TransactionFileRepository catches
     *                                   this so one bad block does not
     *                                   stop the rest of the file loading.
     */
    public static Transaction fromFileLines(List<String> blockLines) {
        if (blockLines == null || blockLines.isEmpty()) {
            throw new IllegalArgumentException("Empty transaction block.");
        }

        String header = blockLines.get(0);
        if (!header.startsWith("TRANSACTION,")) {
            throw new IllegalArgumentException("Transaction block must start with a TRANSACTION line.");
        }

        String[] parts = header.split(",", -1);
        if (parts.length != 11) {
            throw new IllegalArgumentException("Malformed TRANSACTION line: " + header);
        }

        String transactionId = parts[1].trim();
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(parts[2].trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date/time in transaction " + transactionId, e);
        }
        String cashier = parts[3].trim();
        double subtotal = parseDouble(parts[4], "subtotal", transactionId);
        double discountAmount = parseDouble(parts[5], "discount", transactionId);
        double taxAmount = parseDouble(parts[6], "tax", transactionId);
        double total = parseDouble(parts[7], "total", transactionId);
        String paymentMethod = parts[8].trim();
        double amountPaid = parseDouble(parts[9], "amountPaid", transactionId);
        double change = parseDouble(parts[10], "change", transactionId);

        if (transactionId.isEmpty()) {
            throw new IllegalArgumentException("Transaction is missing its ID.");
        }

        List<CartItem> items = new ArrayList<>();
        boolean sawEnd = false;

        for (int i = 1; i < blockLines.size(); i++) {
            String line = blockLines.get(i);
            if (line.equals("END")) {
                sawEnd = true;
                break;
            }
            if (!line.startsWith("ITEM,")) {
                throw new IllegalArgumentException("Unexpected line in transaction " + transactionId + ": " + line);
            }
            items.add(parseItemLine(line, transactionId));
        }

        if (!sawEnd) {
            throw new IllegalArgumentException("Transaction " + transactionId + " is missing its END line.");
        }

        return new Transaction(transactionId, dateTime, cashier, items, subtotal,
                discountAmount, taxAmount, total, paymentMethod, amountPaid, change);
    }

    /**
     * Parses one ITEM line back into a CartItem. Since a Transaction only
     * needs to REMEMBER what was bought (not manage live stock), a small
     * snapshot Product is rebuilt from the saved id/name/category/price -
     * minimumStock is not needed here, so it defaults to 0.
     */
    private static CartItem parseItemLine(String line, String transactionId) {
        String[] parts = line.split(",", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Malformed ITEM line in transaction " + transactionId + ": " + line);
        }
        String productId = parts[1].trim();
        String productName = parts[2].trim();
        Category category = Category.fromString(parts[3].trim());
        double price = parseDouble(parts[4], "item price", transactionId);
        int quantity;
        try {
            quantity = Integer.parseInt(parts[5].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid item quantity in transaction " + transactionId, e);
        }
        if (productId.isEmpty()) {
            throw new IllegalArgumentException("Item is missing a product ID in transaction " + transactionId);
        }

        Product snapshotProduct = new Product(productId, productName, category, price, quantity, 0);
        return new CartItem(snapshotProduct, quantity);
    }

    private static double parseDouble(String text, String fieldName, String transactionId) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName + " in transaction " + transactionId, e);
        }
    }

    @Override
    public String toString() {
        return "Transaction{id=" + transactionId + ", dateTime=" + dateTime + ", cashier=" + cashier +
                ", total=" + total + ", paymentMethod=" + paymentMethod + "}";
    }
}
