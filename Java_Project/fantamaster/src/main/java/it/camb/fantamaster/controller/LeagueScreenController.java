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
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueScreenController {

    @FXML private StackPane contentArea;
    @FXML private Label leagueNameLabel;
    @FXML private MenuItem scambiMenuItem; 

    private League currentLeague;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        if (leagueNameLabel != null && league != null) {
            leagueNameLabel.setText(league.getName().toUpperCase());
        }
        checkTradeNotifications();
    }

    private void checkTradeNotifications() {
        if (currentLeague == null || SessionUtil.getCurrentSession() == null) return;
        
        try {
            Connection conn = ConnectionFactory.getConnection();
            RosaDAO rosaDAO = new RosaDAO(conn);
            Rosa miaRosa = rosaDAO.getRosaByUserAndLeague(SessionUtil.getCurrentSession().getUser().getId(), currentLeague.getId());
            
            if (miaRosa != null) {
                ScambiDAO scambiDAO = new ScambiDAO(conn);
                int pending = scambiDAO.countRichiestePendenti(miaRosa.getId());
                
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
        } catch (SQLException e) { e.printStackTrace(); }
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
            popup.setOnHidden(e -> checkTradeNotifications());
            popup.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void openAuction() { System.out.println("Asta"); }
    @FXML private void openChat() { try { Parent v = FXMLLoader.load(getClass().getResource("/fxml/ChatView.fxml")); contentArea.getChildren().setAll(v); } catch (IOException e) {} }
    @FXML private void showImpostazioniLega() { try { FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/leagueRules.fxml")); Parent v = l.load(); ((LeagueRulesController)l.getController()).setCurrentLeague(currentLeague); contentArea.getChildren().setAll(v); } catch (IOException e) {} }
    @FXML private void handleShareLeague() { if (currentLeague != null) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Codice"); a.setContentText(currentLeague.getInviteCode()); a.showAndWait(); } }
    @FXML private void handleQuickFormation() { System.out.println("Formazione"); }
    @FXML private void handleShowResults() { System.out.println("Risultati"); }
    @FXML private void goBackToLeagueList() { try { Main.showHome(); } catch (IOException e) {} }
}