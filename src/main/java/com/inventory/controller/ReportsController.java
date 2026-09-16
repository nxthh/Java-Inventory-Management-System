package com.inventory.controller;

import com.inventory.Main;
import com.inventory.model.Product;
import com.inventory.model.Transaction;
import com.inventory.repository.ProductFileRepository;
import com.inventory.repository.TransactionFileRepository;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import com.inventory.service.ReportService;
import com.inventory.service.TransactionService;
import com.inventory.util.Session;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for reports.fxml - the ADMIN-only Reports screen, made up of
 * three tabs: Inventory Report, Sales Report, and Low Stock Report.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * Exactly like every other controller in this project, this class only
 * knows about JavaFX controls and *what* the admin wants to see. It never
 * reads a file or sums numbers itself - it asks ReportService for
 * already-calculated numbers/lists and displays them.
 *
 * Permissions: this whole screen is ADMIN-only. The Dashboard already
 * hides/disables the "Reports" button for a CASHIER, but this controller
 * checks Session.isAdmin() again when the screen loads (the same
 * "defense in depth" pattern InventoryController uses), in case this
 * screen is ever reached another way.
 */
public class ReportsController {

    // ----- Inventory Report tab -----
    @FXML
    private Label totalInventoryValueLabel;
    @FXML
    private TableView<Product> inventoryTable;
    @FXML
    private TableColumn<Product, String> invProductColumn;
    @FXML
    private TableColumn<Product, String> invCategoryColumn;
    @FXML
    private TableColumn<Product, String> invPriceColumn;
    @FXML
    private TableColumn<Product, Integer> invQuantityColumn;
    @FXML
    private TableColumn<Product, String> invValueColumn;

    // ----- Sales Report tab -----
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private Label totalTransactionsLabel;
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label totalDiscountLabel;
    @FXML
    private Label totalTaxLabel;
    @FXML
    private Label salesStatusLabel;

    // ----- Low Stock Report tab -----
    @FXML
    private TableView<Product> lowStockTable;
    @FXML
    private TableColumn<Product, String> lowStockProductColumn;
    @FXML
    private TableColumn<Product, Integer> lowStockQuantityColumn;
    @FXML
    private TableColumn<Product, Integer> lowStockMinColumn;
    @FXML
    private TableColumn<Product, String> lowStockStatusColumn;

    // The controller only talks to services - never straight to a
    // Repository or straight to a file.
    private final ProductFileRepository productFileRepository = new ProductFileRepository();
    private final ProductService productService = new ProductService(productFileRepository);
    private final InventoryService inventoryService = new InventoryService(productFileRepository);

    private final TransactionFileRepository transactionFileRepository = new TransactionFileRepository();
    private final TransactionService transactionService = new TransactionService(transactionFileRepository);

    private final ReportService reportService =
            new ReportService(productService, inventoryService, transactionService);

    // All transactions the current user is allowed to see, loaded once
    // when the screen opens and re-used every time the date filter
    // changes (no need to reload the file just to re-filter dates).
    private List<Transaction> allTransactions;

    @FXML
    private void initialize() {
        if (!Session.isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Reports are only available to ADMIN users.", ButtonType.OK);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.showAndWait();
            handleBack();
            return;
        }

        setupInventoryTable();
        setupLowStockTable();

        refreshInventoryReport();
        refreshLowStockReport();

        allTransactions = reportService.getAllVisibleTransactions();
        showSalesReport(allTransactions);
    }

    // ===================== Inventory Report =====================

    private void setupInventoryTable() {
        invProductColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        invCategoryColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory().toDisplayString()));
        invPriceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrice())));
        invQuantityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        invValueColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%.2f", reportService.getInventoryValue(data.getValue()))));
    }

    @FXML
    private void handleRefreshInventoryReport() {
        refreshInventoryReport();
    }

    /**
     * Reloads every product (via ProductService, underneath ReportService)
     * and recalculates each row's inventory value (price x quantity) plus
     * the grand total inventory value.
     */
    private void refreshInventoryReport() {
        List<Product> products = productService.getAllProducts();
        inventoryTable.setItems(FXCollections.observableArrayList(products));
        totalInventoryValueLabel.setText(String.format("Total Inventory Value: $%.2f",
                reportService.getTotalInventoryValue()));
    }

    // ===================== Sales Report =====================

    /**
     * Called when "Apply Filter" is clicked. An empty DatePicker means
     * "no bound on that side" (ReportService.filterByDateRange already
     * treats a null date that way), so leaving one or both pickers empty
     * still works cleanly instead of causing an error.
     */
    @FXML
    private void handleApplyDateFilter() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from != null && to != null && from.isAfter(to)) {
            salesStatusLabel.setText("\"From\" date must not be after \"To\" date.");
            return;
        }

        List<Transaction> filtered = reportService.filterByDateRange(allTransactions, from, to);
        showSalesReport(filtered);
    }

    @FXML
    private void handleClearDateFilter() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        showSalesReport(allTransactions);
    }

    /**
     * Fills in the four Sales Report labels from whichever transaction
     * list is currently relevant (all of them, or a date-filtered
     * subset). All four numbers come straight from ReportService, which
     * sums Transaction.getTotal()/getDiscountAmount()/getTaxAmount().
     */
    private void showSalesReport(List<Transaction> transactions) {
        totalTransactionsLabel.setText(
                "Total Transactions: " + reportService.getTotalTransactionCount(transactions));
        totalRevenueLabel.setText(
                String.format("Total Revenue: $%.2f", reportService.getTotalRevenue(transactions)));
        totalDiscountLabel.setText(
                String.format("Total Discount: $%.2f", reportService.getTotalDiscount(transactions)));
        totalTaxLabel.setText(
                String.format("Total Tax: $%.2f", reportService.getTotalTax(transactions)));
        salesStatusLabel.setText(transactions.isEmpty() ? "No transactions found for this range." : "");
    }

    // ===================== Low Stock Report =====================

    private void setupLowStockTable() {
        lowStockProductColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        lowStockQuantityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        lowStockMinColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getMinimumStock()).asObject());
        lowStockStatusColumn.setCellValueFactory(data -> new SimpleStringProperty("LOW STOCK"));

        // Same red/bold styling InventoryController uses for its Status column.
        lowStockStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                }
            }
        });
    }

    @FXML
    private void handleRefreshLowStockReport() {
        refreshLowStockReport();
    }

    /**
     * Shows only products where quantity <= minimumStock, using
     * InventoryService's rule (via ReportService) - the exact same rule
     * already used by the Inventory screen's "LOW STOCK" status, so this
     * report can never disagree with it.
     */
    private void refreshLowStockReport() {
        List<Product> lowStockProducts = reportService.getLowStockProducts();
        lowStockTable.setItems(FXCollections.observableArrayList(lowStockProducts));
    }

    // ===================== Navigation =====================

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("view/dashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
