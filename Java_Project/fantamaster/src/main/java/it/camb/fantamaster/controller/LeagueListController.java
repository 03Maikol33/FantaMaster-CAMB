package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.faOptionalaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public class LeagueListController {
import javafx.scene.control.Alert;

import javafx.scene.control.TextInputDialog;
    @FXML private VBox leagueContainer;

    @FXML
    public void initialize() {
        loadLeagues();
    }

    private void loadLeagues() {
        // 1. Pulisci e mostra il caricamento
        leagueContainer.getChildren().clear();
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        VBox spinnerBox = new VBox(spinner);
        spinnerBox.setAlignment(Pos.CENTER);
        spinnerBox.setPrefHeight(200); // Spazio vuoto per centrarlo
        
        leagueContainer.getChildren().add(spinnerBox);

        // 2. Esegui il lavoro pesante in un thread separato (BACKGROUND)
        CompletableFuture.supplyAsync(() -> {
            try {
                // NOTA: Qui siamo in un thread parallelo. Niente UI qui!
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                User currentUser = SessionUtil.getCurrentSession().getUser();
                return leagueDAO.getLeaguesForUser(currentUser);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.<League>emptyList();
            }
        }).thenAccept(leagues -> {
            // 3. Quando i dati sono pronti, torniamo al thread JavaFX (UI)
            Platform.runLater(() -> {
                // Rimuoviamo lo spinner
                leagueContainer.getChildren().remove(spinnerBox);

                // Popoliamo la lista
                for (League league : leagues) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueListItem.fxml"));
                        Node item = loader.load();
                        LeagueListItemController controller = loader.getController();
                        controller.setLeague(league);
                        leagueContainer.getChildren().add(item);
                    } catch (IOException e) {
                        e.printStackTrace();
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
            
            // Ricarica la lista dopo la chiusura
            loadLeagues();
        } catch (IOException e) {
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
// Fix conflitti definitivo
