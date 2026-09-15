package com.inventory.controller;

import com.inventory.Main;
import javafx.fxml.FXML;

import java.io.IOException;

/**
 * Controller for dashboard.fxml.
 *
 * Phase 1 only needs this to exist and support navigating back to the
 * Login screen. Inventory features (add/edit/delete/search products, etc.)
 * will be added to this controller in Phase 3.
 */
public class DashboardController {

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
