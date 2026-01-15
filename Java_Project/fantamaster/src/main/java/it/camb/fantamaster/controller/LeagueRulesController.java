package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

public class LeagueRulesController {

    @FXML private FlowPane modulesContainer;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private League currentLeague;
    
    // Lista dei moduli standard del sistema
    private final List<String> ALL_MODULES = Arrays.asList(
        "3-4-3", "3-5-2", "4-5-1", "4-4-2", "4-3-3", "5-4-1", "5-3-2"
    );

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        loadCurrentRules();
    }

    private void loadCurrentRules() {
        modulesContainer.getChildren().clear();
        
        // Recupera i moduli già attivi dalla lega (o usa tutti se è la prima volta/null)
        String dbModules = currentLeague.getAllowedFormations();
        List<String> activeModules = (dbModules != null && !dbModules.isEmpty()) 
            ? Arrays.asList(dbModules.split(",")) 
            : new ArrayList<>();

        // Crea un bottone per ogni modulo possibile
        for (String module : ALL_MODULES) {
            ToggleButton btn = new ToggleButton(module);
            
            // Stile base (puoi spostarlo nel CSS)
            btn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 15; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-cursor: hand;");
            
            btn.setPrefWidth(80);
            
            // Logica visuale selezione
            btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    // Stile Selezionato (Blu/Verde)
                    btn.setStyle("-fx-background-color: #3b82f6; -fx-border-color: #3b82f6; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 15; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(59, 130, 246, 0.3), 5, 0, 0, 2);");
                    errorLabel.setVisible(false);
                } else {
                    // Stile Deselezionato
                    btn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 15; -fx-text-fill: #64748b; -fx-font-weight: bold;");
                }
            });

            // Se è attivo nel DB, selezionalo
            if (activeModules.contains(module) || activeModules.isEmpty()) { 
               // Nota: se activeModules è vuoto (nuova lega), magari li attiviamo tutti di default o nessuno. 
               // Qui ho messo che se li trova nel DB li attiva.
               if (activeModules.contains(module)) btn.setSelected(true);
            }

            modulesContainer.getChildren().add(btn);
        }
    }

    @FXML
    private void handleSaveRules() {
        // 1. Raccogli i moduli selezionati
        List<String> selectedModules = modulesContainer.getChildren().stream()
                .filter(node -> node instanceof ToggleButton)
                .map(node -> (ToggleButton) node)
                .filter(ToggleButton::isSelected)
                .map(ToggleButton::getText)
                .collect(Collectors.toList());

        // 2. Validazione
        if (selectedModules.isEmpty()) {
            errorLabel.setVisible(true);
            return;
        }

        // 3. Converti in stringa CSV
        String csvModules = String.join(",", selectedModules);

        // 4. Salva nel DB
        try {
            Connection conn = ConnectionFactory.getConnection();
            LeagueDAO dao = new LeagueDAO(conn);
            
            if (dao.updateLeagueRules(currentLeague.getId(), csvModules)) {
                // Aggiorna modello locale
                currentLeague.setAllowedFormations(csvModules);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Salvataggio");
                alert.setHeaderText(null);
                alert.setContentText("Regole aggiornate con successo!");
                alert.showAndWait();
            } else {
                showError("Errore nel salvataggio su DB.");
            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore salvataggio regole lega", e);
            showError("Errore di connessione.");
        }
    }
    
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }
}