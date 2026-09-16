package com.inventory.util;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;

/**
 * ReceiptDialog is a small, reusable helper that shows a receipt's plain
 * text in a simple popup window (a TextArea inside a Dialog), so the
 * cashier can read it right after checkout - or later, from the
 * Transaction History screen.
 *
 * OOP concept: this is a UI HELPER, not a Service or Repository - it only
 * knows how to display text that has already been generated. It never
 * builds receipt text itself (that is ReceiptService's job) and never
 * touches a file (that is ReceiptFileRepository's job). Keeping this tiny
 * bit of JavaFX code in one shared place means both POSController and
 * TransactionController can reuse it instead of duplicating the same
 * Dialog-building code twice.
 */
public class ReceiptDialog {

    // Private constructor: every method here is static, so this class is
    // used as ReceiptDialog.show(...) and never instantiated.
    private ReceiptDialog() {
    }

    /**
     * Opens a simple dialog window showing the given receipt text.
     */
    public static void show(String title, String receiptText) {
        TextArea textArea = new TextArea(receiptText);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefColumnCount(40);
        textArea.setPrefRowCount(24);
        textArea.setStyle("-fx-font-family: 'monospace';");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
