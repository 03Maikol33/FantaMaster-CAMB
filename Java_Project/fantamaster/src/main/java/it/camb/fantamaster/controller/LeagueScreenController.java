package it.camb.fantamaster.controller;

import java.io.IOException;
import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueScreenController {

    @FXML private StackPane contentArea;
    @FXML private Label leagueNameLabel;

    private League currentLeague;

    /**
     * Inizializza la schermata con i dati della lega selezionata.
     */
    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        if (leagueNameLabel != null) {
            leagueNameLabel.setText(league.getName().toUpperCase());
        }
        
        
    }

    // --- METODI NAVBAR INFERIORE (Azioni Principali) ---

    @FXML
    private void openSquadra() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/squadra.fxml"));
            Parent view = loader.load();
            
            SquadraController controller = loader.getController();
            controller.initData(currentLeague); 
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openChat() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/ChatView.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_menu.fxml"));
            Parent view = loader.load();

            StatisticsMenuController controller = loader.getController();
            controller.initData(currentLeague, contentArea);

            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- METODI MENU A TENDINA (Azioni Secondarie e Gestionali) ---

    @FXML
    private void openAuction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionMainContainer.fxml"));
            Parent view = loader.load();
            
            AuctionMainContainerController controller = loader.getController();
            controller.initData(currentLeague.getId()); 
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showListone() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/listone.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showScambi() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/trades.fxml"));
            Parent root = loader.load();
            
            TradesController controller = loader.getController();
            controller.setLeague(currentLeague);

            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Mercato Scambi - " + currentLeague.getName());
            popup.setScene(new Scene(root));
            popup.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShareLeague() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Codice Lega");
        alert.setHeaderText("Codice di invito:");
        alert.setContentText(currentLeague.getInviteCode());
        alert.showAndWait();
    }

    // --- NAVIGAZIONE GENERALE ---

    @FXML
    private void goBackToLeagueList() {
        try {
            Main.showHome();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}