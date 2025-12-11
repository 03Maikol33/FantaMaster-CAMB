package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.sql.SQLException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RulesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LeagueAdminSettingsController {
    
    // Elementi UI - Zona Pericolosa
    @FXML private Button closeRegistrationsButton;
    @FXML private Label closeRegistrationsWarningLabel;
    
    // Budget
    @FXML private TextField budgetField;
    
    // Bonus
    @FXML private TextField bonusGolField;
    @FXML private TextField bonusAssistField;
    @FXML private TextField bonusRigoreParatoField;
    @FXML private TextField bonusImbattibilitaField;
    @FXML private TextField bonusFattoreCampoField;
    
    // Opzioni
    @FXML private CheckBox modificatoreDifesaCheck;
    
    // Malus
    @FXML private TextField malusGolSubitoField;
    @FXML private TextField malusAutogolField;
    @FXML private TextField malusRigoreSbagliatoField;
    @FXML private TextField malusEspulsioneField;
    @FXML private TextField malusAmmonizioneField;

    private League currentLeague;
    private Rules currentRules;

    /**
     * Metodo chiamato dall'esterno per impostare la lega corrente.
     * Avvia il caricamento dei dati aggiornati dal DB.
     */
    public void setCurrentLeague(League league) {
        // Usiamo l'ID per scaricare la versione più aggiornata della lega e delle regole
        refreshLeagueData(league.getId());
    }

    private void refreshLeagueData(int leagueId) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            RulesDAO rulesDAO = new RulesDAO(conn);
            
            this.currentLeague = leagueDAO.getLeagueById(leagueId);
            this.currentRules = rulesDAO.getRulesByLeagueId(leagueId);
            
            if (this.currentLeague != null && this.currentRules != null) {
                updateUI();
            } else {
                showAlert(AlertType.ERROR, "Errore", "Impossibile trovare i dati della lega.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Errore Database", "Impossibile aggiornare i dati.");
        }
    }

    /**
     * Aggiorna i campi dell'interfaccia con i valori caricati.
     */
    private void updateUI() {
        if (currentLeague == null || currentRules == null) return;

        // 1. Budget
        budgetField.setText(String.valueOf(currentRules.getInitialBudget()));

        // 2. Bonus & Opzioni
        bonusGolField.setText(String.valueOf(currentRules.getBonusGol()));
        bonusAssistField.setText(String.valueOf(currentRules.getBonusAssist()));
        bonusRigoreParatoField.setText(String.valueOf(currentRules.getBonusRigoreParato()));
        bonusImbattibilitaField.setText(String.valueOf(currentRules.getBonusImbattibilita()));
        bonusFattoreCampoField.setText(String.valueOf(currentRules.getBonusFattoreCampo()));
        
        modificatoreDifesaCheck.setSelected(currentRules.isUsaModificatoreDifesa());

        // 3. Malus
        malusGolSubitoField.setText(String.valueOf(currentRules.getMalusGolSubito()));
        malusAutogolField.setText(String.valueOf(currentRules.getMalusAutogol()));
        malusRigoreSbagliatoField.setText(String.valueOf(currentRules.getMalusRigoreSbagliato()));
        malusEspulsioneField.setText(String.valueOf(currentRules.getMalusEspulsione()));
        malusAmmonizioneField.setText(String.valueOf(currentRules.getMalusAmmonizione()));

        // 4. Gestione Bottone Chiusura Iscrizioni
        if (currentLeague.isRegistrationsClosed()) {
            disableCloseButton("Le iscrizioni sono già chiuse.", true);
        } else if (!isParticipantCountEven()) {
            disableCloseButton("Partecipanti dispari (" + currentLeague.getParticipants().size() + ").", true);
        } else {
            enableCloseButton();
        }
    }

    /**
     * Gestisce il salvataggio di TUTTE le impostazioni (Budget, Bonus, Malus).
     */
    @FXML
    private void handleSaveRules() {
        try {
            // Validazione Budget
            int budget = Integer.parseInt(budgetField.getText());
            if (budget < 250) throw new NumberFormatException("Budget troppo basso");

            // Validazione & Parsing Bonus/Malus
            double bGol = parseAndValidate(bonusGolField.getText());
            double bAssist = parseAndValidate(bonusAssistField.getText());
            double bRigPar = parseAndValidate(bonusRigoreParatoField.getText());
            double bImbat = parseAndValidate(bonusImbattibilitaField.getText());
            double bCampo = parseAndValidate(bonusFattoreCampoField.getText());

            double mGolSub = parseAndValidate(malusGolSubitoField.getText());
            double mAutogol = parseAndValidate(malusAutogolField.getText());
            double mRigSba = parseAndValidate(malusRigoreSbagliatoField.getText());
            double mEspul = parseAndValidate(malusEspulsioneField.getText());
            double mAmmo = parseAndValidate(malusAmmonizioneField.getText());

            // Aggiornamento Oggetto Rules
            currentRules.setInitialBudget(budget);
            currentRules.setUsaModificatoreDifesa(modificatoreDifesaCheck.isSelected());
            
            currentRules.setBonusGol(bGol);
            currentRules.setBonusAssist(bAssist);
            currentRules.setBonusRigoreParato(bRigPar);
            currentRules.setBonusImbattibilita(bImbat);
            currentRules.setBonusFattoreCampo(bCampo);
            
            currentRules.setMalusGolSubito(mGolSub);
            currentRules.setMalusAutogol(mAutogol);
            currentRules.setMalusRigoreSbagliato(mRigSba);
            currentRules.setMalusEspulsione(mEspul);
            currentRules.setMalusAmmonizione(mAmmo);

            // Salvataggio DB
            Connection conn = ConnectionFactory.getConnection();
            RulesDAO rulesDAO = new RulesDAO(conn);
            
            if (rulesDAO.updateRules(currentLeague.getId(), currentRules)) {
                showAlert(AlertType.INFORMATION, "Successo", "Impostazioni aggiornate con successo!");
            } else {
                showAlert(AlertType.ERROR, "Errore", "Salvataggio fallito.");
            }

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Errore Input", "Inserisci solo numeri validi (usa il punto per i decimali, es: 0.5).");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Errore DB", "Problema di connessione.");
        }
    }

    /**
     * Chiude le iscrizioni alla lega (irreversibile).
     */
    @FXML
    public void handleCloseRegistrations() {
        refreshLeagueData(currentLeague.getId());

        if (currentLeague != null && isParticipantCountEven()) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                
                if (leagueDAO.closeRegistrations(currentLeague.getId())) {
                    currentLeague.setRegistrationsClosed(true);
                    showAlert(AlertType.INFORMATION, "Successo", "Iscrizioni chiuse con successo.");
                    updateUI(); 
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Errore", "Errore durante la chiusura delle iscrizioni.");
            }
        } else {
            showAlert(AlertType.WARNING, "Attenzione", "Impossibile chiudere: il numero di partecipanti è dispari o la lega non esiste.");
        }
    }

    /**
     * Elimina definitivamente la lega.
     */
    @FXML
    public void handleDeleteLeague() {
        if (currentLeague != null) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                if(leagueDAO.deleteLeague(currentLeague.getId())) {
                    showAlert(AlertType.INFORMATION, "Successo", "Lega eliminata con successo: " + currentLeague.getName());
                    
                    // Chiudi la finestra corrente e torna alla home
                    Stage stage = (Stage) closeRegistrationsButton.getScene().getWindow();
                    stage.close(); 
                    Main.showHome();

                } else {
                    showAlert(AlertType.ERROR, "Errore", "Errore durante l'eliminazione della lega.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Errore", "Eccezione durante l'eliminazione.");
            }
        }
    }

    // --- Metodi Helper ---

    private double parseAndValidate(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        double val = Double.parseDouble(text.replace(",", "."));
        if (val < 0) throw new NumberFormatException("I valori devono essere positivi");
        return val;
    }

    private boolean isParticipantCountEven() {
        return currentLeague.getParticipants() != null && currentLeague.getParticipants().size() % 2 == 0;
    }

    private void disableCloseButton(String msg, boolean info) {
        closeRegistrationsButton.setDisable(true);
        closeRegistrationsWarningLabel.setText(msg);
        closeRegistrationsWarningLabel.setStyle("-fx-text-fill: #718096;"); // Grigio
    }
    
    private void enableCloseButton() {
        closeRegistrationsButton.setDisable(false);
        closeRegistrationsWarningLabel.setText("Attenzione: azione irreversibile.");
        closeRegistrationsWarningLabel.setStyle("-fx-text-fill: #e53e3e;"); // Rosso
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}