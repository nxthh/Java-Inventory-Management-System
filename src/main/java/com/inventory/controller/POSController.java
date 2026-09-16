package com.inventory.controller;

import com.inventory.Main;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidCartOperationException;
import com.inventory.exception.InvalidDiscountException;
import com.inventory.exception.PaymentException;
import com.inventory.model.Cart;
import com.inventory.model.CartItem;
import com.inventory.model.CheckoutTotals;
import com.inventory.model.Discount;
import com.inventory.model.PercentageDiscount;
import com.inventory.model.Product;
import com.inventory.model.Transaction;
import com.inventory.model.payment.CardPayment;
import com.inventory.model.payment.CashPayment;
import com.inventory.model.payment.Payment;
import com.inventory.model.payment.QRPayment;
import com.inventory.repository.ProductFileRepository;
import com.inventory.repository.ReceiptFileRepository;
import com.inventory.repository.TransactionFileRepository;
import com.inventory.service.CheckoutService;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import com.inventory.service.ReceiptService;
import com.inventory.service.TaxCalculator;
import com.inventory.exception.ReceiptException;
import com.inventory.util.ReceiptDialog;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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

    // ----- Checkout section -----
    @FXML
    private TextField discountField;
    @FXML
    private Label discountAmountLabel;
    @FXML
    private Label taxAmountLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private ComboBox<String> paymentMethodComboBox;
    @FXML
    private TextField amountPaidField;
    @FXML
    private Label changeLabel;
    @FXML
    private Button checkoutButton;

    // The Controller talks only to services for product/stock/checkout
    // work - never straight to a Repository or straight to a file.
    private final ProductFileRepository productFileRepository = new ProductFileRepository();
    private final ProductService productService = new ProductService(productFileRepository);
    private final InventoryService inventoryService = new InventoryService(productFileRepository);

    // Part 6A: handles all reading/writing of data/transactions.txt, so
    // this controller never has to touch File I/O directly.
    private final TransactionFileRepository transactionFileRepository = new TransactionFileRepository();

    // Part 6B: handles all reading/writing of receipt files under
    // data/receipts/, and building the receipt's text layout - again,
    // this controller never touches a file or formats a receipt itself.
    private final ReceiptFileRepository receiptFileRepository = new ReceiptFileRepository();
    private final ReceiptService receiptService = new ReceiptService(receiptFileRepository);

    // The store's tax rate lives in exactly ONE place. To change the tax
    // rate for the whole application, change this one number.
    private final TaxCalculator taxCalculator = new TaxCalculator(0.10); // 10%

    private final CheckoutService checkoutService = new CheckoutService(
            productService, inventoryService, taxCalculator, transactionFileRepository, receiptService);

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
        setupPaymentMethodComboBox();
        setupCheckoutListeners();
        refreshProducts();
        refreshCartView(); // also triggers the first recalculateTotals()
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
     * Fills the Payment ComboBox with the three supported methods and
     * reacts when the cashier changes their selection: the Amount Paid
     * field only makes sense for Cash, so it is disabled (and cleared)
     * for Card/QR.
     */
    private void setupPaymentMethodComboBox() {
        paymentMethodComboBox.setItems(FXCollections.observableArrayList("Cash", "Card", "QR"));
        paymentMethodComboBox.getSelectionModel().select("Cash");

        paymentMethodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCash = "Cash".equals(newVal);
            amountPaidField.setDisable(!isCash);
            if (!isCash) {
                amountPaidField.clear();
            }
            recalculateTotals();
        });
    }

    /**
     * As the cashier types a discount or an amount paid, the Discount/
     * Tax/Total/Change labels should update live, without needing to
     * click anything first.
     */
    private void setupCheckoutListeners() {
        discountField.textProperty().addListener((obs, oldVal, newVal) -> recalculateTotals());
        amountPaidField.textProperty().addListener((obs, oldVal, newVal) -> recalculateTotals());
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
        checkoutButton.setDisable(cart.isEmpty());
        recalculateTotals();
    }

    /**
     * Recomputes Discount/Tax/Total/Change from the cart's current
     * contents plus whatever the cashier has typed/selected on the
     * checkout panel, and refreshes those labels. This is only a PREVIEW
     * - it never touches stock or a payment, it just shows the cashier
     * what checkout would currently charge.
     */
    private void recalculateTotals() {
        double discountPercent;
        try {
            discountPercent = parseDiscountPercent();
        } catch (InvalidDiscountException e) {
            // Still typing (e.g. field temporarily empty or "-") - just
            // preview with no discount instead of showing an alert.
            discountPercent = 0;
        }

        Discount discount = new PercentageDiscount(discountPercent);
        CheckoutTotals totals = checkoutService.calculateTotals(cart, discount);

        discountAmountLabel.setText("Discount: $" + String.format("%.2f", totals.getDiscountAmount()));
        taxAmountLabel.setText("Tax: $" + String.format("%.2f", totals.getTaxAmount()));
        totalLabel.setText("TOTAL: $" + String.format("%.2f", totals.getTotal()));

        if ("Cash".equals(paymentMethodComboBox.getValue())) {
            double amountPaid = parseAmountPaidLenient();
            double change = Math.max(amountPaid - totals.getTotal(), 0);
            changeLabel.setText("Change: $" + String.format("%.2f", change));
        } else {
            changeLabel.setText("Change: $0.00");
        }
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

    /**
     * Runs the full checkout process for the current cart:
     * builds a Discount and a Payment from what the cashier entered, then
     * hands both to CheckoutService, which validates everything, takes
     * the payment, deducts stock, and returns a Transaction. Any problem
     * along the way (empty cart, insufficient stock, bad discount, bad
     * payment amount, insufficient cash) is shown as an Alert and stops
     * the checkout - stock and the cart are left untouched.
     */
    @FXML
    private void handleCheckout() {
        try {
            if (cart.isEmpty()) {
                showError("The cart is empty. Add a product before checking out.");
                return;
            }

            double discountPercent = parseDiscountPercent();
            Discount discount = new PercentageDiscount(discountPercent);

            CheckoutTotals totals = checkoutService.calculateTotals(cart, discount);

            String method = paymentMethodComboBox.getValue();
            Payment payment = buildPayment(method, totals.getTotal());

            Transaction transaction = checkoutService.checkout(cart, discount, payment);

            showCheckoutSuccess(transaction);
            showReceipt(transaction);
            resetCheckoutForm();
            refreshProducts();  // stock changed - reload the product table
            refreshCartView();  // cart is now empty

        } catch (InvalidCartOperationException | InsufficientStockException
                | InvalidDiscountException | PaymentException | ReceiptException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Loads the receipt that was just saved for this transaction and
     * shows it in a simple popup so the cashier can read it right away.
     *
     * This re-reads the receipt from disk (via ReceiptService) instead of
     * keeping the text around in memory, which doubles as a quick check
     * that the receipt really was saved correctly. If, for some reason,
     * it cannot be read back (see ReceiptException), the sale itself is
     * NOT undone - the transaction and stock changes already happened -
     * the cashier just sees a friendly error instead of the receipt text.
     */
    private void showReceipt(Transaction transaction) {
        try {
            String receiptText = receiptService.loadReceiptText(transaction.getReceiptId());
            ReceiptDialog.show("Receipt " + transaction.getReceiptId(), receiptText);
        } catch (ReceiptException e) {
            showError("The sale was completed, but the receipt could not be displayed: " + e.getMessage());
        }
    }

    /**
     * Creates the right kind of Payment for the selected method.
     *
     * OOP concept: POLYMORPHISM. The return type is the general "Payment"
     * type - the caller (handleCheckout) never needs an if/else per
     * payment type after this point; it just calls payment.processPayment().
     */
    private Payment buildPayment(String method, double total) {
        if ("Cash".equals(method)) {
            double amountPaid = parseAmountPaid();
            return new CashPayment(total, amountPaid);
        } else if ("Card".equals(method)) {
            return new CardPayment(total);
        } else {
            return new QRPayment(total);
        }
    }

    /**
     * Parses the discount field strictly - used right before checkout,
     * where an invalid value should stop checkout with a clear message.
     *
     * @throws InvalidDiscountException if the text is missing, not a
     *                                   number, or outside 0-100
     */
    private double parseDiscountPercent() {
        String text = discountField.getText() == null ? "" : discountField.getText().trim();
        if (text.isEmpty()) {
            return 0;
        }
        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new InvalidDiscountException("Discount must be a number.");
        }
        if (value < 0 || value > 100) {
            throw new InvalidDiscountException("Discount must be between 0 and 100.");
        }
        return value;
    }

    /**
     * Parses the amount paid field strictly - used right before checkout.
     *
     * @throws PaymentException if the text is missing, not a number, or negative
     */
    private double parseAmountPaid() {
        String text = amountPaidField.getText() == null ? "" : amountPaidField.getText().trim();
        if (text.isEmpty()) {
            throw new PaymentException("Please enter the amount paid.");
        }
        double amount;
        try {
            amount = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new PaymentException("Amount paid must be a valid number.");
        }
        if (amount < 0) {
            throw new PaymentException("Amount paid cannot be negative.");
        }
        return amount;
    }

    /**
     * A forgiving version of parseAmountPaid() used only for the LIVE
     * Change preview: while the cashier is still typing, the field may be
     * temporarily empty or invalid, and that should not show an alert -
     * it should just preview as if $0.00 had been paid so far.
     */
    private double parseAmountPaidLenient() {
        try {
            return parseAmountPaid();
        } catch (PaymentException e) {
            return 0;
        }
    }

    /**
     * Shows a summary Alert confirming the sale completed successfully.
     */
    private void showCheckoutSuccess(Transaction transaction) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Transaction ID: %s%n", transaction.getTransactionId()));
        message.append(String.format("Subtotal: $%.2f%n", transaction.getSubtotal()));
        message.append(String.format("Discount: $%.2f%n", transaction.getDiscountAmount()));
        message.append(String.format("Tax: $%.2f%n", transaction.getTaxAmount()));
        message.append(String.format("Total: $%.2f%n", transaction.getTotal()));
        message.append(String.format("Payment Method: %s%n", transaction.getPaymentMethod()));
        if ("Cash".equals(transaction.getPaymentMethod())) {
            message.append(String.format("Amount Paid: $%.2f%n", transaction.getAmountPaid()));
        }
        message.append(String.format("Change: $%.2f", transaction.getChange()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION, message.toString(), ButtonType.OK);
        alert.setTitle("Checkout Complete");
        alert.setHeaderText("Sale completed successfully");
        alert.showAndWait();
    }

    /**
     * Resets the checkout panel back to its defaults after a successful
     * sale, ready for the next customer.
     */
    private void resetCheckoutForm() {
        discountField.setText("0");
        paymentMethodComboBox.getSelectionModel().select("Cash");
        amountPaidField.setDisable(false);
        amountPaidField.clear();
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
