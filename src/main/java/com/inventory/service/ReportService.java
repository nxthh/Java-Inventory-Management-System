package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportService calculates all the numbers shown on the Dashboard and on
 * the Reports screen (total products, total revenue, low stock counts,
 * etc.).
 *
 * OOP concept: LAYERED ARCHITECTURE / REUSE, NOT DUPLICATION.
 * This class does NOT read files and does NOT re-implement rules that
 * already exist elsewhere. It simply ASKS the existing services for data
 * it already knows how to produce:
 *   - ProductService     -> the list of all products
 *   - InventoryService   -> which products are low stock
 *   - TransactionService -> which transactions the current user may see
 *     (ADMIN sees every transaction, CASHIER only sees their own sales -
 *     that rule already lives in TransactionService, so a report built
 *     from it automatically follows the same rule instead of repeating
 *     the permission check here)
 *
 * ReportService then just does simple arithmetic (counting and summing)
 * on top of that data. Keeping this arithmetic in one place means the
 * Dashboard and the Reports screen can never disagree with each other -
 * they both call the exact same methods.
 */
public class ReportService {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final TransactionService transactionService;

    public ReportService(ProductService productService,
                          InventoryService inventoryService,
                          TransactionService transactionService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.transactionService = transactionService;
    }

    // ===================== Inventory numbers =====================

    /**
     * Total number of DIFFERENT products in the catalog (not how many
     * units are in stock - just how many product rows exist).
     */
    public int getTotalProducts() {
        return productService.getAllProducts().size();
    }

    /**
     * Total number of individual units across every product, e.g. 49
     * Coca Colas + 7 Breads + ... This is what "Total Inventory Items"
     * means on the Dashboard.
     */
    public int getTotalInventoryQuantity() {
        int total = 0;
        for (Product product : productService.getAllProducts()) {
            total += product.getQuantity();
        }
        return total;
    }

    /**
     * Every product that is at or below its minimum stock level.
     * Reuses InventoryService.getLowStockProducts() - the exact same
     * rule already used by the Inventory screen's "LOW STOCK" status -
     * instead of re-checking quantity <= minimumStock here again.
     */
    public List<Product> getLowStockProducts() {
        return inventoryService.getLowStockProducts();
    }

    public int getLowStockCount() {
        return getLowStockProducts().size();
    }

    /**
     * The value of one product's stock on hand: price x quantity.
     */
    public double getInventoryValue(Product product) {
        return product.getPrice() * product.getQuantity();
    }

    /**
     * The value of the entire inventory: the sum of price x quantity
     * for every product.
     */
    public double getTotalInventoryValue() {
        double total = 0;
        for (Product product : productService.getAllProducts()) {
            total += getInventoryValue(product);
        }
        return total;
    }

    // ===================== Transaction / sales numbers =====================

    /**
     * Every transaction the CURRENTLY LOGGED IN user is allowed to see
     * (ADMIN: all of them, CASHIER: only their own sales). This is the
     * unfiltered list used by the Sales Report before any date range is
     * applied, and also what the Dashboard's "Total Transactions" and
     * "Total Revenue" are based on.
     */
    public List<Transaction> getAllVisibleTransactions() {
        return transactionService.getVisibleTransactions();
    }

    /**
     * Keeps only the transactions whose date falls within
     * [startDate, endDate] (inclusive). Either bound may be null to
     * leave that side open (e.g. startDate == null means "from the
     * beginning").
     */
    public List<Transaction> filterByDateRange(List<Transaction> transactions,
                                                LocalDate startDate, LocalDate endDate) {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction transaction : transactions) {
            LocalDate saleDate = transaction.getDateTime().toLocalDate();
            boolean notBeforeStart = (startDate == null) || !saleDate.isBefore(startDate);
            boolean notAfterEnd = (endDate == null) || !saleDate.isAfter(endDate);
            if (notBeforeStart && notAfterEnd) {
                filtered.add(transaction);
            }
        }
        return filtered;
    }

    public int getTotalTransactionCount(List<Transaction> transactions) {
        return transactions.size();
    }

    public double getTotalRevenue(List<Transaction> transactions) {
        double total = 0;
        for (Transaction transaction : transactions) {
            total += transaction.getTotal();
        }
        return total;
    }

    public double getTotalDiscount(List<Transaction> transactions) {
        double total = 0;
        for (Transaction transaction : transactions) {
            total += transaction.getDiscountAmount();
        }
        return total;
    }

    public double getTotalTax(List<Transaction> transactions) {
        double total = 0;
        for (Transaction transaction : transactions) {
            total += transaction.getTaxAmount();
        }
        return total;
    }
}
