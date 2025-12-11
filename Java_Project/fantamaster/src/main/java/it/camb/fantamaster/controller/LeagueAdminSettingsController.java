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
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class LeagueAdminSettingsController {
    
    // UI Elements
    @FXML private Button closeRegistrationsButton;
    @FXML private Label closeRegistrationsWarningLabel;
    @FXML private TextField budgetField; 
    @FXML private FlowPane modulesContainer; // Contenitore per i moduli

    // Data
    private League currentLeague;
    private LeagueAdminScreenController parentController;
    
    // Lista moduli disponibili
    private final List<String> ALL_MODULES = Arrays.asList(
        "3-4-3", "3-5-2", "4-5-1", "4-4-2", "4-3-3", "5-4-1", "5-3-2"
    );

    // Stili CSS
    private final String STYLE_UNSELECTED = "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 6 12; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-cursor: hand;";
    private final String STYLE_SELECTED = "-fx-background-color: linear-gradient(to bottom, #1e40af, #3b82f6); -fx-border-color: #1911b5ff; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 6 12; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(128, 90, 213, 0.4), 5, 0, 0, 2);";

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
            this.currentLeague = leagueDAO.getLeagueById(leagueId);
            
            if (this.currentLeague != null) {
                updateUI();
                loadModules(); // Carica i moduli dinamicamente
            } else {
                showAlert("Errore", "Impossibile trovare la lega nel database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore Database", "Impossibile aggiornare i dati della lega.");
        }
    }

    @FXML
    private void updateUI() {
        if (currentLeague == null) return;

        // Budget
        if (budgetField != null) {
            budgetField.setText(String.valueOf(currentLeague.getInitialBudget()));
        }

        // Chiusura Iscrizioni
        if (currentLeague.isRegistrationsClosed()) {
            disableCloseButton("Le iscrizioni sono già chiuse.", true);
        } else if (!isParticipantCountEven()) {
            disableCloseButton("Serve numero pari di utenti per chiudere.", true);
        } else {
            enableCloseButton();
        }
    }

    /**
     * Crea i pulsanti per i moduli e li aggiunge al FlowPane
     */
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
            
            // Logica visuale al click
            btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
                btn.setStyle(newVal ? STYLE_SELECTED : STYLE_UNSELECTED);
            });

            // Imposta stato iniziale
            if (activeModules.contains(module)) {
                btn.setSelected(true);
            }

            modulesContainer.getChildren().add(btn);
        }
    }

    /**
     * Salva i moduli selezionati
     */
    @FXML
    private void handleSaveModules() {
        // Raccogli moduli selezionati
        List<String> selected = modulesContainer.getChildren().stream()
                .filter(n -> n instanceof ToggleButton)
                .map(n -> (ToggleButton) n)
                .filter(ToggleButton::isSelected)
                .map(ToggleButton::getText)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showAlert("Attenzione", "Seleziona almeno un modulo!");
            return;
        }

        String csvModules = String.join(",", selected);

        try {
            Connection conn = ConnectionFactory.getConnection();
            LeagueDAO dao = new LeagueDAO(conn);
            
            if (dao.updateLeagueRules(currentLeague.getId(), csvModules)) {
                currentLeague.setAllowedFormations(csvModules);
                showAlert("Successo", "Moduli aggiornati correttamente!");
                // Qui non "chiudiamo" la finestra perché siamo dentro le impostazioni,
                // ma l'Alert conferma l'azione.
            } else {
                showAlert("Errore", "Errore nel salvataggio su DB.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore", "Errore di connessione.");
        }
    }

    @FXML
    private void handleSaveBudget() {
        String input = budgetField.getText();
        if (!input.matches("\\d+")) {
            showAlert("Errore", "Il budget deve essere un numero intero.");
            return;
        }
        int newBudget = Integer.parseInt(input);
        if (newBudget < 500) {
            showAlert("Errore", "Il budget deve essere di almeno 500 crediti.");
            return;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            RulesDAO rulesDAO = new RulesDAO(conn);
            if (rulesDAO.updateBudget(currentLeague.getId(), newBudget)) {
                currentLeague.setInitialBudget(newBudget);
                showAlert("Successo", "Budget aggiornato!");
                refreshLeagueData(currentLeague.getId());
            } else {
                showAlert("Errore", "Impossibile aggiornare il budget.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore", "Errore DB.");
        }
    }

    // --- Metodi Gestione Lega ---

    private boolean isParticipantCountEven() {
        return currentLeague.getParticipants().size() % 2 == 0;
    }

    @FXML
    public void handleCloseRegistrations() {
        if (currentLeague != null && isParticipantCountEven()) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                if (leagueDAO.closeRegistrations(currentLeague.getId())) {
                    currentLeague.setRegistrationsClosed(true);
                    showAlert("Successo", "Iscrizioni chiuse!");
                    updateUI(); 
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Errore", "Errore chiusura iscrizioni.");
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
                    showAlert("Successo", "Lega eliminata.");
                    Stage stage = (Stage) closeRegistrationsButton.getScene().getWindow();
                    stage.close(); 
                    Main.showHome();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Errore", "Errore eliminazione.");
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

    @FXML
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}