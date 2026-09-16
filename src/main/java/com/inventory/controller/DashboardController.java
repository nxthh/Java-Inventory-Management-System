package com.inventory.controller;

import com.inventory.Main;
import com.inventory.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;

/**
 * Controller for dashboard.fxml.
 *
 * Phase 1 only needed this to exist and support navigating back to the
 * Login screen. Phase 3 adds a button that opens the Inventory screen.
 *
 * OOP concept: this controller does not know HOW inventory is stored or
 * validated - it only knows how to switch the JavaFX scene. All of that
 * other logic lives in InventoryController -> ProductService/InventoryService
 * -> ProductFileRepository.
 */
public class DashboardController {

    @FXML
    private Button inventoryButton;

    /**
     * Called automatically when this screen first loads.
     * CASHIER users cannot manage inventory, so their Inventory button is
     * disabled here (they never see the option at all, per project rules).
     * The POS button has no such restriction: both ADMIN and CASHIER are
     * allowed to use the Point of Sale screen.
     */
    @FXML
    private void initialize() {
        inventoryButton.setDisable(!Session.isAdmin());
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
