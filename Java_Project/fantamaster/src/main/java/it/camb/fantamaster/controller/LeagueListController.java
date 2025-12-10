package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public class LeagueListController {

    @FXML private VBox leagueContainer;

    @FXML
    public void initialize() {
        // ottengo la lista delle leghe per l'utente loggato usando ConnectionFactory
        List<League> leagues;
        try (java.sql.Connection conn = ConnectionFactory.getConnection()) { // usa la mia ConnectionFactory in package Util
            UsersLeaguesDAO usersLeaguesDAO = new UsersLeaguesDAO(conn);
            leagues = usersLeaguesDAO.getLeaguesForUser(SessionUtil.getCurrentSession().getUser());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            leagues = Collections.emptyList();
        }

        for (League league : leagues) {
            System.out.println("Caricamento lega: " + league);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueListItem.fxml"));
                Node item = loader.load();

                LeagueListItemController controller = loader.getController();
                controller.setLeague(league);
                //controller.setLeagueData();

                leagueContainer.getChildren().add(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void openCreateLeague() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createLeague.fxml"));
            Node createLeagueNode = loader.load();

            // Apre la finestra di creazione lega
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Crea Nuova Lega");
            stage.setScene(new javafx.scene.Scene((javafx.scene.Parent) createLeagueNode));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();  
        }
    }
  

    @FXML
    private void handleJoinByCode() {
    // 1. Creiamo un dialog per chiedere il codice
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Unisciti a una Lega");
    dialog.setHeaderText("Inserisci il codice invito della lega");
    dialog.setContentText("Codice:");

    Optional<String> result = dialog.showAndWait();

    result.ifPresent(code -> {
        if (code.trim().isEmpty()) return;

        try (Connection conn = ConnectionFactory.getConnection()) {
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            RequestDAO requestDAO = new RequestDAO(conn);
            
            // 2. Cerchiamo la lega
            League league = leagueDAO.findLeagueByInviteCode(code.trim().toUpperCase()); // Assumiamo codici maiuscoli

            if (league == null) {
                showAlert(Alert.AlertType.ERROR, "Errore", "Codice non valido", "Nessuna lega trovata con questo codice.");
                return;
            }

            // 3. Controlli vari (Già iscritto? Lega piena? Iscrizioni chiuse?)
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

            // 4. Inviamo la richiesta
            if (requestDAO.createRequest(currentUser, league)) {
                showAlert(Alert.AlertType.INFORMATION, "Successo", "Richiesta Inviata", "La richiesta di iscrizione è stata inviata all'admin.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Errore", "Errore Generico", "Impossibile inviare la richiesta. Forse ne hai già inviata una?");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Errore", "Errore di Connessione", "Impossibile connettersi al database.");
        }
    });
}

// Metodo utility per alert
private void showAlert(Alert.AlertType type, String title, String header, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
}
}
