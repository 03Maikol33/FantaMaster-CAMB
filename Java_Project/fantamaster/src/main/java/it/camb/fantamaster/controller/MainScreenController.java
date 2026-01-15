package it.camb.fantamaster.controller;

import it.camb.fantamaster.util.ErrorUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainScreenController {
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        // Carica la vista di benvenuto all'inizio
        showWelcome();
    }

    @FXML
    private void showLeghe() {
        loadView("/fxml/leagueList.fxml");
    }

    @FXML
    private void showProfilo() {
        loadView("/fxml/profilo.fxml");
    }

    @FXML
    private void showWelcome() {
        loadView("/fxml/welcome.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            ErrorUtil.log("Errore nel caricamento della vista: " + fxmlPath, e);
        }
    }
}
