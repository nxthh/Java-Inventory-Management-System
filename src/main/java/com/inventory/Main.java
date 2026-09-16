package com.inventory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlFile));
        Scene scene = new Scene(loader.load());

        // Applying the shared stylesheet here, in ONE place, means every
        // screen gets the same look automatically - no .fxml file needs
        // its own <stylesheets> line, and changing the theme never means
        // hunting through six different files.
        scene.getStylesheets().add(Main.class.getResource("view/style.css").toExternalForm());

        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
