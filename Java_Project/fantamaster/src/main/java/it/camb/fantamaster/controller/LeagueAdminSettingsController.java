package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.sql.SQLException;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class LeagueAdminSettingsController {
    @FXML private Button closeRegistrationsButton;
    @FXML private Label closeRegistrationsWarningLabel;

    private League currentLeague;

    public void setCurrentLeague(League league) {
        // Non ci fidiamo dell'oggetto passato (potrebbe essere vecchio).
        // Usiamo il suo ID per scaricare la versione più fresca dal DB.
        refreshLeagueData(league.getId());
    }

    /**
     * Ricarica la lega dal database per assicurarsi di avere i partecipanti aggiornati.
     */
    private void refreshLeagueData(int leagueId) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            
            // Grazie al refactoring di LeagueDAO, questo metodo carica anche i partecipanti!
            this.currentLeague = leagueDAO.getLeagueById(leagueId);
            
            if (this.currentLeague != null) {
                updateUI();
            } else {
                showAlert("Errore", "Impossibile trovare la lega nel database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore Database", "Impossibile aggiornare i dati della lega.");
        }
    }

    private void updateUI() {
        if (currentLeague == null) return;

        // 1. Controllo se le iscrizioni sono già chiuse
        if (currentLeague.isRegistrationsClosed()) {
            disableCloseButton("Le iscrizioni sono già chiuse per questa lega.", true);
            return;
        }

        // 2. Controllo la regola di business (Numero Pari)
        if (!isParticipantCountEven()) {
            disableCloseButton("Puoi chiudere le iscrizioni solo se il numero di partecipanti è pari (" + currentLeague.getParticipants().size() + ").", true);
        } else {
            enableCloseButton();
        }
    }

    /**
     * Logica di Business: verifica se il numero di partecipanti è pari.
     */
    private boolean isParticipantCountEven() {
        return currentLeague.getParticipants().size() % 2 == 0;
    }

    @FXML
    public void handleCloseRegistrations() {
        // Refresh di sicurezza all'ultimo secondo prima di scrivere
        refreshLeagueData(currentLeague.getId());

        if (currentLeague != null && isParticipantCountEven()) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                
                if (leagueDAO.closeRegistrations(currentLeague.getId())) {
                    currentLeague.setRegistrationsClosed(true);
                    showAlert("Successo", "Iscrizioni chiuse con successo per la lega: " + currentLeague.getName());
                    updateUI(); // Aggiorna l'interfaccia
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Errore", "Errore durante la chiusura delle iscrizioni.");
            }
        } else {
            showAlert("Attenzione", "Impossibile chiudere: il numero di partecipanti è dispari o la lega non esiste.");
        }
    }

    // --- Metodi Helper per UI ---

    private void disableCloseButton(String message, boolean isInfo) {
        closeRegistrationsButton.setDisable(true);
        closeRegistrationsButton.getStyleClass().add("disabled-action-color");
        closeRegistrationsButton.getStyleClass().removeAll("irreversible-action-color");
        
        closeRegistrationsWarningLabel.setText(message);
        closeRegistrationsWarningLabel.getStyleClass().removeAll("irreversible-action-color", "info-action-color");
        closeRegistrationsWarningLabel.getStyleClass().add("info-action-color");
    }

    private void enableCloseButton() {
        closeRegistrationsButton.setDisable(false);
        closeRegistrationsButton.getStyleClass().removeAll("disabled-action-color");
        closeRegistrationsButton.getStyleClass().add("irreversible-action-color");
        
        closeRegistrationsWarningLabel.setText("Attenzione: questa azione è irreversibile e impedirà a nuovi utenti di iscriversi alla lega.");
        closeRegistrationsWarningLabel.getStyleClass().removeAll("info-action-color");
        closeRegistrationsWarningLabel.getStyleClass().add("irreversible-action-color");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}