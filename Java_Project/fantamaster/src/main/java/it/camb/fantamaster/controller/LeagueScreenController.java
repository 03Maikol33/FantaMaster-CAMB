package it.camb.fantamaster.controller;

import java.io.IOException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.SessionUtil;
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
import it.camb.fantamaster.controller.SquadraController;

public class LeagueScreenController {

    @FXML private StackPane contentArea;
    @FXML private Label leagueNameLabel;
    @FXML private Button impostazioniButton; // Assicurati che questo ID sia presente nel tuo leagueScreen.fxml

    private League currentLeague;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        if (leagueNameLabel != null) {
            leagueNameLabel.setText(league.getName().toUpperCase());
        }

        // --- GESTIONE BOTTONE IMPOSTAZIONI ---
        // Recupero l'utente corrente dalla sessione
        User currentUser = SessionUtil.getCurrentSession().getUser();
        
        // Se l'utente non è l'amministratore (creatore), nascondo il tasto
        if (currentLeague.getCreator().getId() != currentUser.getId()) {
            impostazioniButton.setVisible(false);
            impostazioniButton.setManaged(false); // Rimuove lo spazio occupato dal bottone nel layout
        }
    }

    // --- METODI NAVBAR INFERIORE ---

    @FXML
    private void openAuction() {
        try {
            // Carico l'asta dentro la contentArea invece di aprire una nuova finestra
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionMainContainer.fxml"));
            Parent view = loader.load();
            
            AuctionMainContainerController controller = loader.getController();
            controller.initData(currentLeague.getId()); // Inizializzo l'asta con l'ID della lega
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

       @FXML
    private void openSquadra() {
        try {
            // Carico l'asta dentro la contentArea invece di aprire una nuova finestra
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/squadra.fxml"));
            Parent view = loader.load();
            SquadraController controller = loader.getController();
            controller.initData(currentLeague); // Inizializzo l'asta con l'ID della lega
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
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
    private void openStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_menu.fxml"));
            Parent view = loader.load();

            // Recupero il controller del menu statistiche
            StatisticsMenuController controller = loader.getController();
            
            // Passo la lega corrente e il riferimento all'area centrale per la navigazione successiva
            controller.initData(currentLeague, contentArea);

            // Mostro la view dentro contentArea
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
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
    private void showImpostazioniLega() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueRules.fxml"));
            Parent view = loader.load();
            LeagueRulesController controller = loader.getController();
            
            controller.setCurrentLeague(currentLeague); 
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- METODI MENU TENDINA ---

    @FXML
    private void handleShareLeague() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Codice Lega");
        alert.setHeaderText("Codice di invito:");
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackToLeagueList() {
        try {
            Main.showHome();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}