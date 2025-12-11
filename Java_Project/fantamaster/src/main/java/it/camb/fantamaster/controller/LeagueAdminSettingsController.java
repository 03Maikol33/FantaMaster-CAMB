package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class LeagueAdminSettingsController {
    
    // --- UI Elements ---
    @FXML private FlowPane modulesContainer;
    
    // Budget & Opzioni
    @FXML private TextField budgetField;
    @FXML private CheckBox modificatoreDifesaCheck;
    
    // Bonus
    @FXML private TextField bonusGolField;
    @FXML private TextField bonusAssistField;
    @FXML private TextField bonusRigoreParatoField;
    @FXML private TextField bonusImbattibilitaField;
    @FXML private TextField bonusFattoreCampoField;
    
    // Malus
    @FXML private TextField malusGolSubitoField;
    @FXML private TextField malusAutogolField;
    @FXML private TextField malusRigoreSbagliatoField;
    @FXML private TextField malusEspulsioneField;
    @FXML private TextField malusAmmonizioneField;

    // Zona Pericolosa
    @FXML private Button closeRegistrationsButton;
    @FXML private Label closeRegistrationsWarningLabel;

    // Dati
    private League currentLeague;
    private Rules currentRules;
    private LeagueAdminScreenController parentController;
    
    private final List<String> ALL_MODULES = Arrays.asList(
        "3-4-3", "3-5-2", "4-5-1", "4-4-2", "4-3-3", "5-4-1", "5-3-2"
    );

    // Stili CSS
    private final String STYLE_UNSELECTED = "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 6 12; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-cursor: hand;";
    private final String STYLE_SELECTED = "-fx-background-color: linear-gradient(to bottom, #1e40af, #3b82f6); -fx-border-color: #1e40af; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 6 12; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(30, 64, 175, 0.3), 5, 0, 0, 2);";

    public void setCurrentLeague(League league) {
        refreshLeagueData(league.getId());
    }

    public void setParentController(LeagueAdminScreenController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void refreshLeagueData(int leagueId) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            RulesDAO rulesDAO = new RulesDAO(conn);
            
            this.currentLeague = leagueDAO.getLeagueById(leagueId);
            this.currentRules = rulesDAO.getRulesByLeagueId(leagueId);
            
            // Fallback per leghe vecchie senza regole
            if (this.currentRules == null) {
                this.currentRules = new Rules();
                this.currentRules.setLeagueId(leagueId);
            }
            
            if (this.currentLeague != null) {
                updateUI();
                loadModules(); 
            } else {
                showAlert(AlertType.ERROR, "Errore", "Impossibile trovare i dati della lega.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Errore Database", "Impossibile aggiornare i dati.");
        }
    }

    private void updateUI() {
        if (currentLeague == null || currentRules == null) return;
        
        // 1. Budget & Opzioni
        budgetField.setText(String.valueOf(currentRules.getInitialBudget()));
        if (modificatoreDifesaCheck != null) {
            modificatoreDifesaCheck.setSelected(currentRules.isUsaModificatoreDifesa());
        }

        // 2. Bonus
        bonusGolField.setText(String.valueOf(currentRules.getBonusGol()));
        bonusAssistField.setText(String.valueOf(currentRules.getBonusAssist()));
        bonusRigoreParatoField.setText(String.valueOf(currentRules.getBonusRigoreParato()));
        bonusImbattibilitaField.setText(String.valueOf(currentRules.getBonusImbattibilita()));
        bonusFattoreCampoField.setText(String.valueOf(currentRules.getBonusFattoreCampo()));

        // 3. Malus
        malusGolSubitoField.setText(String.valueOf(currentRules.getMalusGolSubito()));
        malusAutogolField.setText(String.valueOf(currentRules.getMalusAutogol()));
        malusRigoreSbagliatoField.setText(String.valueOf(currentRules.getMalusRigoreSbagliato()));
        malusEspulsioneField.setText(String.valueOf(currentRules.getMalusEspulsione()));
        malusAmmonizioneField.setText(String.valueOf(currentRules.getMalusAmmonizione()));

        // 4. Bottone Chiusura Iscrizioni
        if (currentLeague.isRegistrationsClosed()) {
            disableCloseButton("Le iscrizioni sono già chiuse.", true);
        } else if (!isParticipantCountEven()) {
            disableCloseButton("Partecipanti dispari (" + currentLeague.getParticipants().size() + ").", true);
        } else {
            enableCloseButton();
        }
    }

    private void loadModules() {
        if (modulesContainer == null) return;
        modulesContainer.getChildren().clear();
        
        String dbModules = currentLeague.getAllowedFormations();
        List<String> activeModules = (dbModules != null && !dbModules.isEmpty()) 
            ? Arrays.asList(dbModules.split(",")) 
            : new ArrayList<>();

        for (String module : ALL_MODULES) {
            ToggleButton btn = new ToggleButton(module);
            btn.setStyle(STYLE_UNSELECTED);
            
            btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
                btn.setStyle(newVal ? STYLE_SELECTED : STYLE_UNSELECTED);
            });

            if (activeModules.contains(module)) {
                btn.setSelected(true);
            }
            modulesContainer.getChildren().add(btn);
        }
    }

    /**
     * UNICO METODO DI SALVATAGGIO: Salva Moduli + Regole
     */
    @FXML
    private void handleSaveRules() {
        try {
            // --- 1. Validazione & Parsing ---
            int budget = Integer.parseInt(budgetField.getText());
            if (budget < 250) throw new NumberFormatException("Il budget deve essere almeno 250.");

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

            // Moduli
            List<String> selectedModules = modulesContainer.getChildren().stream()
                    .filter(n -> n instanceof ToggleButton)
                    .map(n -> (ToggleButton) n)
                    .filter(ToggleButton::isSelected)
                    .map(ToggleButton::getText)
                    .collect(Collectors.toList());

            if (selectedModules.isEmpty()) {
                showAlert(AlertType.WARNING, "Attenzione", "Seleziona almeno un modulo di gioco.");
                return;
            }
            String csvModules = String.join(",", selectedModules);

            // --- 2. Aggiornamento Oggetti in Memoria ---
            currentRules.setInitialBudget(budget);
            if (modificatoreDifesaCheck != null) {
                currentRules.setUsaModificatoreDifesa(modificatoreDifesaCheck.isSelected());
            }
            
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

            // --- 3. Salvataggio su DB ---
            Connection conn = ConnectionFactory.getConnection();
            RulesDAO rulesDAO = new RulesDAO(conn);
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            
            // Salviamo Regole e Moduli
            boolean rulesSaved = rulesDAO.updateRules(currentLeague.getId(), currentRules);
            boolean modulesSaved = leagueDAO.updateLeagueRules(currentLeague.getId(), csvModules);

            if (rulesSaved && modulesSaved) {
                currentLeague.setInitialBudget(budget);
                currentLeague.setAllowedFormations(csvModules);
                showAlert(AlertType.INFORMATION, "Successo", "Tutte le impostazioni salvate correttamente!");
            } else {
                showAlert(AlertType.ERROR, "Errore", "Errore nel salvataggio dei dati.");
            }

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Errore Input", "Inserisci solo numeri validi. " + e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Errore DB", "Problema di connessione al database.");
        }
    }

    // --- Metodi Gestione Lega ---

    private boolean isParticipantCountEven() {
        return currentLeague.getParticipants() != null && currentLeague.getParticipants().size() % 2 == 0;
    }

    @FXML
    public void handleCloseRegistrations() {
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
        }
    }

    @FXML
    public void handleDeleteLeague() {
        if (currentLeague != null) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                if(leagueDAO.deleteLeague(currentLeague.getId())) {
                    showAlert(AlertType.INFORMATION, "Successo", "Lega eliminata.");
                    Stage stage = (Stage) closeRegistrationsButton.getScene().getWindow();
                    stage.close(); 
                    Main.showHome();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Errore", "Errore eliminazione.");
            }
        }
    }

    // --- Helper UI ---

    @FXML
    private void disableCloseButton(String message, boolean isInfo) {
        closeRegistrationsButton.setDisable(true);
        closeRegistrationsButton.getStyleClass().add("disabled-action-color");
        closeRegistrationsButton.getStyleClass().removeAll("irreversible-action-color");
        closeRegistrationsWarningLabel.setText(message);
        closeRegistrationsWarningLabel.getStyleClass().add("info-action-color");
    }

    @FXML
    private void enableCloseButton() {
        closeRegistrationsButton.setDisable(false);
        closeRegistrationsButton.getStyleClass().removeAll("disabled-action-color");
        closeRegistrationsButton.getStyleClass().add("irreversible-action-color");
        closeRegistrationsWarningLabel.setText("Attenzione: questa azione è irreversibile.");
        closeRegistrationsWarningLabel.getStyleClass().removeAll("info-action-color");
        closeRegistrationsWarningLabel.getStyleClass().add("irreversible-action-color");
    }

    private double parseAndValidate(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        double val = Double.parseDouble(text.replace(",", "."));
        if (val < 0) throw new NumberFormatException("I valori devono essere positivi");
        return val;
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("FantaMaster");
        alert.setHeaderText(title);
        alert.setContentText(content);
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/fxml/css/style.css").toExternalForm()
            );
            alert.getDialogPane().getStyleClass().add("dialog-pane");
        } catch (Exception e) { }
        alert.showAndWait();
    }
}