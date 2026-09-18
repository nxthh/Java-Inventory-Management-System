package com.inventory.controller;

import com.inventory.Main;
import com.inventory.repository.ProductFileRepository;
import com.inventory.repository.TransactionFileRepository;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import com.inventory.service.ReportService;
import com.inventory.service.TransactionService;
import com.inventory.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controller for dashboard.fxml.
 *
 * Phase 1 only needed this to exist and support navigating back to the
 * Login screen. Phase 3 added a button that opens the Inventory screen.
 * Phase 7 adds the summary "cards" (Total Products, Inventory Items,
 * Low Stock, Transactions, Total Revenue) and the Reports button.
 *
 * Layout note: the navigation buttons used to sit in a row across the
 * top of the screen. dashboard.fxml now places them in a sidebar on the
 * left instead (top title bar + left sidebar + main content, like a
 * typical admin dashboard). Only the FXML layout changed - every
 * onAction handler below is exactly the same method it always was, so
 * none of this controller's logic needed to change for the new look.
 *
 * OOP concept: this controller does not know HOW inventory or sales
 * numbers are calculated - it only asks ReportService for the finished
 * numbers and puts them into Labels. All of the counting/summing logic
 * lives in ReportService (which itself reuses ProductService,
 * InventoryService, and TransactionService instead of duplicating them).
 */
public class DashboardController {

    @FXML
    private Button inventoryButton;
    @FXML
    private Button reportsButton;
    @FXML
    private Button usersButton;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label totalInventoryItemsLabel;
    @FXML
    private Label lowStockProductsLabel;
    @FXML
    private Label totalTransactionsLabel;
    @FXML
    private Label totalRevenueLabel;

    // The controller only talks to services - never straight to a
    // Repository or straight to a file, same pattern as every other
    // controller in this project.
    private final ProductFileRepository productFileRepository = new ProductFileRepository();
    private final ProductService productService = new ProductService(productFileRepository);
    private final InventoryService inventoryService = new InventoryService(productFileRepository);

    private final TransactionFileRepository transactionFileRepository = new TransactionFileRepository();
    private final TransactionService transactionService = new TransactionService(transactionFileRepository);

    private final ReportService reportService =
            new ReportService(productService, inventoryService, transactionService);

    /**
     * Called automatically when this screen first loads.
     * CASHIER users cannot manage inventory or view admin reports, so
     * those buttons are disabled here (they never see the option at all,
     * per project rules). The POS and Transaction History buttons have
     * no such restriction: both ADMIN and CASHIER are allowed to use
     * them.
     */
    @FXML
    private void initialize() {
        inventoryButton.setDisable(!Session.isAdmin());
        reportsButton.setDisable(!Session.isAdmin());
        usersButton.setDisable(!Session.isAdmin());
        currentUserLabel.setText(Session.getCurrentUsername() + " (" + Session.getCurrentRole() + ")");
        refreshSummary();
    }

    /**
     * Recalculates every summary card from the current saved data
     * (products.txt / transactions.txt) via ReportService. Called on
     * load, and again whenever the "Refresh" button is clicked, so the
     * numbers always reflect the latest sales/stock changes.
     *
     * Note: "Total Transactions" and "Total Revenue" follow the SAME
     * ADMIN-sees-everything / CASHIER-sees-only-their-own-sales rule
     * used by the Transaction History screen, because both come from
     * TransactionService underneath ReportService.
     */
    private void refreshSummary() {
        totalProductsLabel.setText(String.valueOf(reportService.getTotalProducts()));
        totalInventoryItemsLabel.setText(String.valueOf(reportService.getTotalInventoryQuantity()));
        lowStockProductsLabel.setText(String.valueOf(reportService.getLowStockCount()));

        int transactionCount = reportService.getTotalTransactionCount(reportService.getAllVisibleTransactions());
        double revenue = reportService.getTotalRevenue(reportService.getAllVisibleTransactions());
        totalTransactionsLabel.setText(String.valueOf(transactionCount));
        totalRevenueLabel.setText(String.format("$%.2f", revenue));
    }

    /**
     * Called automatically when the "Refresh" button is clicked
     * (linked via onAction="#handleRefreshDashboard" in dashboard.fxml).
     */
    @FXML
    private void handleRefreshDashboard() {
        refreshSummary();
    }

    /**
     * Called automatically when the "Inventory" button is clicked
     * (linked via onAction="#handleOpenInventory" in dashboard.fxml).
     */
    @FXML
    private void handleOpenInventory() {
        try {
            Main.switchScene("view/inventory.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically when the "Point of Sale" button is clicked
     * (linked via onAction="#handleOpenPOS" in dashboard.fxml). Both ADMIN
     * and CASHIER users are allowed to reach this screen.
     */
    @FXML
    private void handleOpenPOS() {
        try {
            Main.switchScene("view/pos.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically when the "Transaction History" button is
     * clicked (linked via onAction="#handleOpenTransactions" in
     * dashboard.fxml). Both ADMIN and CASHIER can open this screen -
     * TransactionController itself decides which transactions each role
     * is actually allowed to SEE (all of them for ADMIN, only their own
     * for CASHIER).
     */
    @FXML
    private void handleOpenTransactions() {
        try {
            Main.switchScene("view/transactions.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically when the "Reports" button is clicked
     * (linked via onAction="#handleOpenReports" in dashboard.fxml).
     * ADMIN-only: the button itself is disabled for CASHIER above, and
     * ReportsController double-checks this again when the screen loads
     * (defense in depth, same pattern InventoryController uses).
     */
    @FXML
    private void handleOpenReports() {
        try {
            Main.switchScene("view/reports.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically when the "Manage Users" button is clicked
     * (linked via onAction="#handleOpenUsers" in dashboard.fxml).
     * ADMIN-only: the button itself is disabled for CASHIER above, and
     * UserController double-checks this again when the screen loads
     * (defense in depth, same pattern InventoryController/ReportsController use).
     */
    @FXML
    private void handleOpenUsers() {
        try {
            Main.switchScene("view/users.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically when the "Logout" button is clicked
     * (linked via onAction="#handleLogout" in dashboard.fxml).
     */
    @FXML
    private void handleLogout() {
        try {
            Main.switchScene("view/login.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
