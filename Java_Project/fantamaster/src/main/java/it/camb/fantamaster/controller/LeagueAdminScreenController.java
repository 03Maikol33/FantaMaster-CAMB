package it.camb.fantamaster.controller;

import java.io.IOException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueAdminScreenController {

    @FXML private StackPane contentArea;
    @FXML private Label leagueNameLabel;
    @FXML private Button richiesteButton;
    @FXML private Button auctionButton;

    private League currentLeague;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        if (leagueNameLabel != null) {
            leagueNameLabel.setText(league.getName().toUpperCase());
        }
    }

    // --- NAVBAR INFERIORE ---

    @FXML
    private void showRichieste() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/requestList.fxml"));
            Parent view = loader.load();
            RequestListController controller = loader.getController();
            controller.setCurrentLeague(currentLeague); 
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void showListone() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/listone.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void openAuction() {
        System.out.println("Apertura Asta");
    }

    @FXML
    private void openChat() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/ChatView.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void showImpostazioniLega() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueAdminSettings.fxml"));
            Parent view = loader.load();
            LeagueAdminSettingsController controller = loader.getController();
            controller.setCurrentLeague(currentLeague);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- MENU AZIONI SUPERIORE (Tutti i partecipanti) ---

    @FXML
    private void handleShareLeague() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Codice Lega");
        alert.setHeaderText("Codice Invito:");
        alert.setContentText(currentLeague.getInviteCode());
        alert.showAndWait();
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
    private void handleQuickFormation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/formation.fxml"));
            Parent root = loader.load();
            FormationController controller = loader.getController();
            controller.setLeague(currentLeague);
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Schiera Formazione");
            popup.setScene(new Scene(root));
            popup.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleShowResults() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/simulated_matchdays.fxml"));
            Parent root = loader.load();
            SimulatedMatchdaysController controller = loader.getController();
            controller.setLeague(currentLeague);
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Risultati Giornate");
            popup.setScene(new Scene(root));
            popup.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void goBackToLeagueList() {
        try { Main.showHome(); } catch (IOException e) { e.printStackTrace(); }
    }
}