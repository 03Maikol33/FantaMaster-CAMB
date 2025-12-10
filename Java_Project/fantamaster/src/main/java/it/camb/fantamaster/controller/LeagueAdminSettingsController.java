package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.sql.SQLException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RulesDAO; // Importante: Importiamo il nuovo DAO
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LeagueAdminSettingsController {
    @FXML private Button closeRegistrationsButton;
    @FXML private Label closeRegistrationsWarningLabel;
    
    // Campo per il budget (definito nel FXML)
    @FXML private TextField budgetField; 

    private League currentLeague;

    public void setCurrentLeague(League league) {
        // Usiamo l'ID per scaricare la versione più aggiornata della lega dal DB
        refreshLeagueData(league.getId());
    }

    /**
     * Ricarica la lega dal database per avere partecipanti e regole aggiornati.
     */
    private void refreshLeagueData(int leagueId) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            
            // Questo metodo ora fa una JOIN e recupera anche il budget dalla tabella regole
            this.currentLeague = leagueDAO.getLeagueById(leagueId);
            
            if (this.currentLeague != null) {
                updateUI();
            } else {
                showAlert( "Errore", "Impossibile trovare la lega nel database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert( "Errore Database", "Impossibile aggiornare i dati della lega.");
        }
    }

    private void updateUI() {
        if (currentLeague == null) return;

        // Popola il campo budget con il valore attuale letto dal DB
        if (budgetField != null) {
            budgetField.setText(String.valueOf(currentLeague.getInitialBudget()));
        }

        // Gestione stato bottone chiusura iscrizioni
        if (currentLeague.isRegistrationsClosed()) {
            disableCloseButton("Le iscrizioni sono già chiuse per questa lega.", true);
            return;
        }

        if (!isParticipantCountEven()) {
            disableCloseButton("Puoi chiudere le iscrizioni solo se il numero di partecipanti è pari (" + currentLeague.getParticipants().size() + ").", true);
        } else {
            enableCloseButton();
        }
    }

    // --- NUOVO METODO: Gestione salvataggio Budget ---
    @FXML
    private void handleSaveBudget() {
        String input = budgetField.getText();

        // 1. Validazione: è un numero intero?
        if (!input.matches("\\d+")) {
            showAlert("Errore", "Il budget deve essere un numero intero.");
            return;
        }

        int newBudget = Integer.parseInt(input);

        // 2. Validazione: rispetta il minimo?
        if (newBudget < 500) {
            showAlert( "Errore", "Il budget deve essere di almeno 500 crediti.");
            return;
        }

        // 3. Persistenza nel DB
        try {
            Connection conn = ConnectionFactory.getConnection();
            
            // CORREZIONE: Usiamo RulesDAO per aggiornare la tabella 'regole'
            RulesDAO rulesDAO = new RulesDAO(conn);
            boolean success = rulesDAO.updateBudget(currentLeague.getId(), newBudget);

            if (success) {
                // Aggiorna il modello locale per riflettere il cambiamento immediatamente
                currentLeague.setInitialBudget(newBudget);
                
                showAlert( "Successo", "Budget aggiornato correttamente a " + newBudget);
                
                // Ricarica i dati per sicurezza
                refreshLeagueData(currentLeague.getId());
            } else {
                showAlert( "Errore", "Impossibile aggiornare il budget nel database.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore Database", "Errore di connessione.");
        }
    }

    // --- Metodi Esistenti ---

    private boolean isParticipantCountEven() {
        return currentLeague.getParticipants().size() % 2 == 0;
    }

    @FXML
    public void handleCloseRegistrations() {
        refreshLeagueData(currentLeague.getId());

        if (currentLeague != null && isParticipantCountEven()) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                
                if (leagueDAO.closeRegistrations(currentLeague.getId())) {
                    currentLeague.setRegistrationsClosed(true);
                    showAlert("Successo", "Iscrizioni chiuse con successo per la lega: " + currentLeague.getName());
                    updateUI(); 
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Errore", "Errore durante la chiusura delle iscrizioni.");
            }
        } else {
            showAlert( "Attenzione", "Impossibile chiudere: il numero di partecipanti è dispari o la lega non esiste.");
        }
    }

    

    @FXML
    public void handleDeleteLeague() {
        if (currentLeague != null) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                if(leagueDAO.deleteLeague(currentLeague.getId())) {
                    showAlert("Successo", "Lega eliminata con successo: " + currentLeague.getName());
                    
                    // Chiudi la finestra corrente e torna alla home
                    Stage stage = (Stage) closeRegistrationsButton.getScene().getWindow();
                    stage.close(); 
                    Main.showHome();

                } else {
                    showAlert("Errore", "Errore durante l'eliminazione della lega.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Errore", "Eccezione durante l'eliminazione.");
            }
        }
    }

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

    // Metodo utility per mostrare alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}