package com.inventory.controller;

import com.inventory.Main;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidCartOperationException;
import com.inventory.model.Cart;
import com.inventory.model.CartItem;
import com.inventory.model.Product;
import com.inventory.repository.ProductFileRepository;
import com.inventory.service.ProductService;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for pos.fxml - the Point of Sale (POS) screen where a cashier
 * builds up a cart of products before checkout (checkout itself is a later
 * phase).
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * Just like InventoryController, this class only knows about JavaFX
 * controls and *what* the cashier wants to do (search, select a product,
 * add/update/remove a cart line, clear the cart). It does not know how
 * products are stored (that is ProductService/ProductFileRepository) and
 * it does not know how cart totals are calculated or how stock limits are
 * enforced (that is Cart's job). This keeps the controller small and easy
 * to read.
 *
 * Architecture for this screen:
 *   POSController -> Cart -> CartItem -> Product
 *   POSController -> ProductService -> ProductFileRepository -> file
 */
public class POSController {

    // ----- Product search/select section -----
    @FXML
    private TextField searchField;
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, String> idColumn;
    @FXML
    private TableColumn<Product, String> nameColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;
    @FXML
    private TableColumn<Product, String> priceColumn;
    @FXML
    private TableColumn<Product, Integer> stockColumn;

    @FXML
    private Label selectedProductLabel;
    @FXML
    private TextField quantityField;
    @FXML
    private Button addToCartButton;

    // ----- Cart section -----
    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private TableColumn<CartItem, String> cartProductColumn;
    @FXML
    private TableColumn<CartItem, Integer> cartQuantityColumn;
    @FXML
    private TableColumn<CartItem, String> cartPriceColumn;
    @FXML
    private TableColumn<CartItem, String> cartSubtotalColumn;

    @FXML
    private TextField updateQuantityField;
    @FXML
    private Button updateQuantityButton;
    @FXML
    private Button removeItemButton;
    @FXML
    private Button clearCartButton;

    @FXML
    private Label subtotalLabel;
    @FXML
    private Label statusMessageLabel;

    // The Controller talks only to ProductService for product lookups -
    // never straight to the Repository or straight to a file.
    private final ProductFileRepository productFileRepository = new ProductFileRepository();
    private final ProductService productService = new ProductService(productFileRepository);

    // The cart for the CURRENT sale. A new POSController (and therefore a
    // new, empty Cart) is created each time the POS screen is opened.
    private final Cart cart = new Cart();

    // The full, unfiltered list of products loaded from the service. The
    // product table only ever shows a filtered copy of this list (see applySearch).
    private List<Product> allProducts = new ArrayList<>();

    // The product currently selected in the product table (null if none).
    private Product selectedProduct;

    // The cart line currently selected in the cart table (null if none).
    private CartItem selectedCartItem;

    /**
     * Called automatically by JavaFX right after pos.fxml is loaded.
     */
    @FXML
    private void initialize() {
        setupProductTableColumns();
        setupCartTableColumns();
        setupProductSelectionListener();
        setupCartSelectionListener();
        setupSearchListener();
        refreshProducts();
        refreshCartView();
        updateActionButtonsState();
    }

    // ===================== Setup helpers =====================

    private void setupProductTableColumns() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        categoryColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory().toDisplayString()));
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f", data.getValue().getPrice())));
        stockColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
    }

    private void setupCartTableColumns() {
        cartProductColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct().getName()));
        cartQuantityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        cartPriceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f", data.getValue().getProduct().getPrice())));
        cartSubtotalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f", data.getValue().getSubtotal())));
    }

    private void setupProductSelectionListener() {
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedProduct = newVal;
            if (newVal != null) {
                selectedProductLabel.setText(
                        "Selected: " + newVal.getName() + " (Stock: " + newVal.getQuantity() + ")");
                quantityField.setText("1");
            } else {
                selectedProductLabel.setText("Selected: (none)");
                quantityField.clear();
            }
            updateActionButtonsState();
        });
    }

    private void setupCartSelectionListener() {
        cartTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCartItem = newVal;
            updateQuantityField.setText(newVal == null ? "" : String.valueOf(newVal.getQuantity()));
            updateActionButtonsState();
        });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch());
    }

    /**
     * Update/Remove only make sense once something is actually selected.
     */
    private void updateActionButtonsState() {
        addToCartButton.setDisable(selectedProduct == null);
        updateQuantityButton.setDisable(selectedCartItem == null);
        removeItemButton.setDisable(selectedCartItem == null);
    }

    // ===================== Data loading / filtering =====================

    private void refreshProducts() {
        allProducts = productService.getAllProducts();
        applySearch();
    }

    /**
     * Filters the cached product list by the search box text (matches
     * either the ID or the name, case-insensitive).
     */
    private void applySearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<Product> filtered = new ArrayList<>();
        for (Product product : allProducts) {
            boolean matches = keyword.isEmpty()
                    || product.getId().toLowerCase().contains(keyword)
                    || product.getName().toLowerCase().contains(keyword);
            if (matches) {
                filtered.add(product);
            }
        }
        productTable.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * Rebuilds the cart table and subtotal label from the Cart object.
     * Called after every cart change so the UI always matches Cart's state.
     */
    private void refreshCartView() {
        cartTable.setItems(FXCollections.observableArrayList(cart.getItems()));
        subtotalLabel.setText("Subtotal: $" + String.format("%.2f", cart.getSubtotal()));
    }

    // ===================== Button actions =====================

    @FXML
    private void handleRefreshProducts() {
        productTable.getSelectionModel().clearSelection();
        refreshProducts();
        statusMessageLabel.setText("");
    }

    @FXML
    private void handleAddToCart() {
        if (selectedProduct == null) {
            showError("Please select a product first.");
            return;
        }
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            cart.addItem(selectedProduct, quantity);
            showSuccess("Added " + quantity + " x " + selectedProduct.getName() + " to cart.");
            refreshCartView();
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        } catch (InvalidCartOperationException | InsufficientStockException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleUpdateQuantity() {
        if (selectedCartItem == null) {
            showError("Please select an item in the cart first.");
            return;
        }
        String productId = selectedCartItem.getProduct().getId();
        try {
            int newQuantity = Integer.parseInt(updateQuantityField.getText().trim());
            cart.updateQuantity(productId, newQuantity);
            showSuccess("Quantity updated.");
            refreshCartView();
            reselectCartItemById(productId); // keep the same row selected after the table reloads
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        } catch (InvalidCartOperationException | InsufficientStockException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleRemoveItem() {
        if (selectedCartItem == null) {
            showError("Please select an item in the cart first.");
            return;
        }
        try {
            cart.removeItem(selectedCartItem.getProduct().getId());
            showSuccess("Item removed from cart.");
            selectedCartItem = null;
            updateQuantityField.clear();
            refreshCartView();
        } catch (InvalidCartOperationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleClearCart() {
        if (cart.isEmpty()) {
            showError("The cart is already empty.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to clear the entire cart?", ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Confirm Clear Cart");
        confirmAlert.setHeaderText(null);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            cart.clear();
            selectedCartItem = null;
            updateQuantityField.clear();
            refreshCartView();
            showSuccess("Cart cleared.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("view/dashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===================== Shared helpers =====================

    /**
     * Re-selects a cart line by product ID after the table data is
     * reloaded (reloading replaces the table's row list, which clears the
     * selection).
     */
    private void reselectCartItemById(String productId) {
        for (CartItem item : cartTable.getItems()) {
            if (item.getProduct().getId().equalsIgnoreCase(productId)) {
                cartTable.getSelectionModel().select(item);
                break;
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        statusMessageLabel.setStyle("-fx-text-fill: #2e7d32;");
        statusMessageLabel.setText(message);
    }
}
