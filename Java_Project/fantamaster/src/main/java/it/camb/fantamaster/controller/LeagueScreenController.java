package it.camb.fantamaster.controller;

import java.io.IOException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LeagueScreenController {
    @FXML private StackPane contentArea;
    private League currentLeague; //tiene il riferimento alla lega attualmente aperta

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        System.out.println("Lega corrente impostata in LeagueAdminScreenController: " + league);
    }
/*
    @FXML
    private void showImpostazioniLega() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueAdminSettings.fxml"));
            Parent view = loader.load();

            // Recupero il controller della schermata richieste
            LeagueAdminSettingsController controller = loader.getController();
            controller.setCurrentLeague(currentLeague); // passo la lega corrente

            // Mostro la view dentro contentArea
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    @FXML
    private void goBackToLeagueList() {
        try{
            Main.showHome();
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



    @FXML
    private void openChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatView.fxml"));
            Parent view = loader.load();

            // Recupero il controller della chat
            ChatViewController controller = loader.getController();
            
            // Passo la lega corrente (usiamo initData che abbiamo creato prima)
            controller.initData(currentLeague); 

            // Mostro la view dentro contentArea (Sostituzione della schermata centrale)
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// Fix conflitti definitivo
