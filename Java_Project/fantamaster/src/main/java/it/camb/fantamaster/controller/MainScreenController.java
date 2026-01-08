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
        loadView("/fxml/profilo.fxml");
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
