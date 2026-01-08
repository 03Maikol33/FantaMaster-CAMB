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
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueScreenController {

    @FXML private StackPane contentArea;
    @FXML private Label leagueNameLabel;
    @FXML private MenuItem impostazioniMenuItem; // Gestito nel menu a tendina

    private League currentLeague;

    /**
     * Inizializza la dashboard della lega.
     * @param league La lega selezionata.
     */
    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        if (leagueNameLabel != null) {
            leagueNameLabel.setText(league.getName().toUpperCase());
        }

        // --- GESTIONE PERMESSI AMMINISTRATORE ---
        User currentUser = SessionUtil.getCurrentSession().getUser();
        boolean isAdmin = currentLeague.getCreator().getId() == currentUser.getId();
        
        if (impostazioniMenuItem != null) {
            impostazioniMenuItem.setVisible(isAdmin);
        }

        // Carica la vista "Squadra" come predefinita all'apertura
        showSquadra();
    }

    // --- METODI NAVBAR INFERIORE (SQUADRA - CHAT - STATS) ---

    @FXML
    private void showSquadra() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/squadra.fxml"));
            Parent view = loader.load();
            
            // Inizializziamo il controller della squadra con la lega corrente
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
    private void showStatistiche() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_menu.fxml"));
            Parent view = loader.load();
            
            StatisticsMenuController controller = loader.getController();
            controller.setLeague(currentLeague);
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- METODI MENU TENDINA (AZIONI E GESTIONE) ---

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
    private void handleShowListone() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/listone.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowRequests() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/requestList.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    @FXML
    private void goBackToLeagueList() {
        try {
            Main.showHome();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}