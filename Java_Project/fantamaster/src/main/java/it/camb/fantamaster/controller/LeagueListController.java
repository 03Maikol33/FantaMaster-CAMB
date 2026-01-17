package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueListController {

    @FXML private VBox leagueContainer;

    /**
     * Inizializza il controller e carica le leghe dell'utente.
     */
    @FXML
    public void initialize() {
        loadLeagues();
    }

    private void loadLeagues() {
        leagueContainer.getChildren().clear();
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        VBox spinnerBox = new VBox(spinner);
        spinnerBox.setAlignment(Pos.CENTER);
        spinnerBox.setPrefHeight(200);
        
        leagueContainer.getChildren().add(spinnerBox);

        CompletableFuture.supplyAsync(() -> {
            // *** FIX: Niente try-with-resources per non chiudere la connessione condivisa ***
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                User currentUser = SessionUtil.getCurrentSession().getUser();
                return leagueDAO.getLeaguesForUser(currentUser);
            } catch (SQLException e) {
                ErrorUtil.log("Errore caricamento leghe per l'utente", e);
                return Collections.<League>emptyList();
            }
        }).thenAccept(leagues -> {
            Platform.runLater(() -> {
                leagueContainer.getChildren().remove(spinnerBox);
                for (League league : leagues) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueListItem.fxml"));
                        Node item = loader.load();
                        LeagueListItemController controller = loader.getController();
                        controller.setLeague(league);
                        leagueContainer.getChildren().add(item);
                    } catch (IOException e) {
                        ErrorUtil.log("Errore caricamento item lista leghe", e);
                    }
                }
            });
        });
    }

    @FXML
    private void openCreateLeague() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createLeague.fxml"));
            Parent createLeagueNode = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Crea Nuova Lega");
            stage.setScene(new Scene(createLeagueNode));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            loadLeagues();
        } catch (IOException e) {
            ErrorUtil.log("Errore apertura finestra creazione lega", e);
        }
    }
  
    @FXML
    private void handleJoinByCode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Unisciti a una Lega");
        dialog.setHeaderText("Inserisci il codice invito della lega");
        dialog.setContentText("Codice:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(code -> {
            if (code.trim().isEmpty()) return;

            // *** FIX: Anche qui, usiamo la connessione senza chiuderla ***
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                RequestDAO requestDAO = new RequestDAO(conn);
                
                League league = leagueDAO.findLeagueByInviteCode(code.trim().toUpperCase());

                if (league == null) {
                    showAlert(Alert.AlertType.ERROR, "Errore", "Codice non valido", "Nessuna lega trovata con questo codice.");
                    return;
                }

                User currentUser = SessionUtil.getCurrentSession().getUser();
                UsersLeaguesDAO ulDao = new UsersLeaguesDAO(conn);
                
                if (ulDao.isUserSubscribed(currentUser, league)) {
                    showAlert(Alert.AlertType.WARNING, "Attenzione", "Sei già iscritto", "Fai già parte di questa lega.");
                    return;
                }
                
                if (league.isRegistrationsClosed()) {
                    showAlert(Alert.AlertType.ERROR, "Errore", "Iscrizioni Chiuse", "Le iscrizioni per questa lega sono chiuse.");
                    return;
                }

                if (requestDAO.createRequest(currentUser, league)) {
                    showAlert(Alert.AlertType.INFORMATION, "Successo", "Richiesta Inviata", "La richiesta di iscrizione è stata inviata all'admin.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Errore", "Errore Generico", "Impossibile inviare la richiesta. Forse ne hai già inviata una?");
                }

            } catch (Exception e) {
                ErrorUtil.log("Errore durante l'iscrizione alla lega tramite codice", e);
                showAlert(Alert.AlertType.ERROR, "Errore", "Errore di Connessione", "Impossibile connettersi al database.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}