package com.inventory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main is the entry point of the JavaFX application.
 *
 * OOP concept: Main extends Application, which is a JavaFX class.
 * This is INHERITANCE - Main "is-a" Application and must implement
 * the start() method that JavaFX requires.
 *
 * Main's job is to:
 *   1. Start the JavaFX application.
 *   2. Show the Login screen first.
 *   3. Provide a simple way for controllers to switch scenes
 *      (Login -> Dashboard, Dashboard -> Inventory/POS/etc.), applying
 *      the shared style.css to each one.
 *
 * Main does NOT contain business logic (like checking a username/password
 * - that lives in AuthService) or File I/O (that lives in the repository
 * classes). Keeping Main this small means the entry point of the whole
 * application stays easy to read at a glance.
 */
public class Main extends Application {

    // Keep a single reference to the main window (the "Stage").
    // Every scene we switch to will be displayed on this same window.
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Inventory Management & POS System");

        switchScene("view/login.fxml");

        primaryStage.setResizable(true);
        primaryStage.show();
    }

    /**
     * Loads an FXML file and displays it as the current scene.
     * Controllers call this method to navigate between screens,
     * e.g. Main.switchScene("view/dashboard.fxml");
     *
     * @param fxmlFile path to the FXML file, relative to the com.inventory package
     */
    public static void switchScene(String fxmlFile) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            // Applying the shared stylesheet here, in ONE place, means every
            // screen gets the same look automatically - no .fxml file needs
            // its own <stylesheets> line, and changing the theme never means
            // hunting through six different files.
            scene.getStylesheets().add(Main.class.getResource("view/style.css").toExternalForm());

            primaryStage.setScene(scene);
        } catch (IOException e) {
            // Before this try/catch, a failure here (e.g. an fx:id typo in
            // the FXML, or an exception thrown inside a controller's
            // initialize() method) was only printed to the IntelliJ
            // console via a caller's e.printStackTrace() - the screen the
            // user clicked on simply never appeared, which looks exactly
            // like "the button does nothing". Showing an Alert here means
            // any future problem like that is immediately visible on
            // screen instead of hiding in the console.
            showLoadError(fxmlFile, e);
            throw e;
        }
    }

    /**
     * Shows a plain error dialog explaining that a screen could not be
     * opened, including the real cause (e.g. "NullPointerException").
     * Kept here, in ONE place, so every screen switch benefits from the
     * same friendly error handling without each controller needing its
     * own copy of this code.
     */
    private static void showLoadError(String fxmlFile, Exception e) {
        Throwable cause = (e.getCause() != null) ? e.getCause() : e;
        Alert alert = new Alert(Alert.AlertType.ERROR,
                "Could not open this screen (" + fxmlFile + ").\n\n"
                        + cause.getClass().getSimpleName()
                        + (cause.getMessage() != null ? ": " + cause.getMessage() : ""),
                ButtonType.OK);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
