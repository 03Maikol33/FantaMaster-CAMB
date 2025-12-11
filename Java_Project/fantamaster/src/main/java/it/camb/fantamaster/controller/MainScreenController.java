package it.camb.fantamaster.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainScreenController {
    @FXML private StackPane contentArea;

    @FXML
    private void showLeghe() {
        loadView("/fxml/leagueList.fxml");
    }

    @FXML
    private void showProfilo() {
        // Profilo UI removed; show placeholder instead
        try {
            Label placeholder = new Label("Funzionalità Profilo non disponibile.");
            placeholder.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
            contentArea.getChildren().setAll(placeholder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
