package it.camb.fantamaster.controller;

import java.sql.SQLException;

import it.camb.fantamaster.dao.RulesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.model.campionato.EventoData;
import it.camb.fantamaster.model.campionato.GiornataData;
import it.camb.fantamaster.model.campionato.MatchData;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class MatchdayDetailController {

    @FXML private Label titleLabel;
    @FXML private VBox eventsContainer;
    
    private League currentLeague;
    private Rules rules;

    public void setData(GiornataData giornata, League league) {
        this.currentLeague = league;
        this.titleLabel.setText("Risultati Giornata " + giornata.giornata);
        
        try {
            RulesDAO rulesDAO = new RulesDAO(ConnectionFactory.getConnection());
            this.rules = rulesDAO.getRulesByLeagueId(league.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            this.rules = new Rules(); // Default se il DB fallisce
        }

        eventsContainer.getChildren().clear();

        for (MatchData match : giornata.partite) {
            VBox matchBox = new VBox(5);
            matchBox.setStyle("-fx-background-color: #2d3748; -fx-padding: 8; -fx-background-radius: 10;");
            
            Label score = new Label(match.casa + " " + match.risultato_casa + " - " + match.risultato_trasferta + " " + match.trasferta);
            score.setStyle("-fx-text-fill: #48bb78; -fx-font-weight: bold; -fx-font-size: 11;");
            matchBox.getChildren().add(score);

            for (EventoData ev : match.eventi) {
                HBox row = new HBox(5);
                row.setAlignment(Pos.CENTER_LEFT);
                
                double val = getValoreAzione(ev.azione);
                String sign = val > 0 ? "+" : "";
                
                Label detail = new Label(ev.nome + " (" + ev.azione.replace("_", " ") + ")");
                detail.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 9;");
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                Label ptsLabel = new Label(sign + val);
                ptsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10;");
                ptsLabel.setTextFill(val >= 0 ? Color.LIGHTGREEN : Color.web("#feb2b2"));

                row.getChildren().addAll(detail, spacer, ptsLabel);
                matchBox.getChildren().add(row);
            }
            eventsContainer.getChildren().add(matchBox);
        }
    }

    private double getValoreAzione(String azione) {
        if (rules == null) return 0.0;
        return switch (azione.toLowerCase()) {
            case "goal" -> rules.getBonusGol();
            case "assist" -> rules.getBonusAssist();
            case "ammonizione" -> -rules.getMalusAmmonizione();
            case "espulsione" -> -rules.getMalusEspulsione();
            case "goal_subito" -> -rules.getMalusGolSubito();
            case "imbattibilita" -> rules.getBonusImbattibilita();
            case "rigore_parato" -> rules.getBonusRigoreParato();
            case "rigore_sbagliato" -> -rules.getMalusRigoreSbagliato();
            case "autogoal" -> -rules.getMalusAutogol();
            default -> 0.0;
        };
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/simulated_matchdays.fxml"));
            Parent root = loader.load();
            
            SimulatedMatchdaysController ctrl = loader.getController();
            ctrl.setLeague(currentLeague);

            eventsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}