package it.camb.fantamaster.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.net.URL;

public class FxmlLoadChecker {
    public static void main(String[] args) {
        System.out.println("Starting FXML load checker...");
        // Initialize JavaFX toolkit
        try {
            Platform.startup(() -> {
                try {
                    loadFxml("/fxml/listone.fxml");
                    loadFxml("/fxml/playerItem.fxml");
                    System.out.println("FXML load checks completed successfully.");
                } catch (Exception e) {
                    System.err.println("Exception while loading FXML:");
                    ErrorUtil.log("Exception while loading FXML", e);
                } finally {
                    Platform.exit();
                }
            });
        } catch (IllegalStateException ex) {
            // Platform already started in this JVM; try to run load on FX thread
            Platform.runLater(() -> {
                try {
                    loadFxml("/fxml/listone.fxml");
                    loadFxml("/fxml/playerItem.fxml");
                    System.out.println("FXML load checks completed successfully (runtime).");
                } catch (Exception e) {
                    System.err.println("Exception while loading FXML (runtime):");
                    ErrorUtil.log("Exception while loading FXML (runtime)", e);
                }
            });
        }
    }

    private static void loadFxml(String resourcePath) throws Exception {
        URL url = FxmlLoadChecker.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        System.out.println("Loading: " + resourcePath + " -> " + url);
        FXMLLoader loader = new FXMLLoader(url);
        loader.load();
        System.out.println("Loaded: " + resourcePath);
    }
}
