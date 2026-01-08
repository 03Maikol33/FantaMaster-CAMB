package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.dao.ScambiDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueAdminScreenController {

    @FXML private StackPane contentArea;
    @FXML private Label leagueNameLabel;
    @FXML private Button richiesteButton;
    @FXML private Button auctionButton;
    @FXML private MenuItem scambiMenuItem; // Assicurati di aggiungere fx:id="scambiMenuItem" nel tuo FXML dell'admin

    private League currentLeague;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        if (leagueNameLabel != null) {
            leagueNameLabel.setText(league.getName().toUpperCase());
        }
        // Attivo il controllo notifiche per lo scambio
        checkTradeNotifications();
    }

    /**
     * Sistema di notifica scambi per l'amministratore.
     */
    private void checkTradeNotifications() {
        if (currentLeague == null || SessionUtil.getCurrentSession() == null) return;
        
        try {
            Connection conn = ConnectionFactory.getConnection();
            Rosa miaRosa = new RosaDAO(conn).getRosaByUserAndLeague(
                SessionUtil.getCurrentSession().getUser().getId(), 
                currentLeague.getId()
            );
            
            if (miaRosa != null) {
                int pending = new ScambiDAO(conn).countRichiestePendenti(miaRosa.getId());
                Platform.runLater(() -> {
                    if (scambiMenuItem != null) {
                        if (pending > 0) {
                            scambiMenuItem.setText("🔄 Scambi (" + pending + " NUOVI!)");
                            scambiMenuItem.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                        } else {
                            scambiMenuItem.setText("🔄 Scambi Mercato");
                            scambiMenuItem.setStyle("");
                        }
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        try {
            // Integrazione Asta nella schermata principale dell'admin
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionMainContainer.fxml"));
            Parent view = loader.load();
            
            AuctionMainContainerController controller = loader.getController();
            controller.initData(currentLeague.getId()); // Inizializzo l'asta
            
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
        } catch (IOException e) { e.printStackTrace(); }
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

  /*   @FXML
    private void showListone() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/listone.fxml"));
            Parent view = loader.load();

            // Mostro la view dentro contentArea
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } */

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

    // --- MENU AZIONI SUPERIORE ---

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
            
            // Quando si chiude la finestra scambi, rinfresca le notifiche
            popup.setOnHidden(e -> checkTradeNotifications());
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