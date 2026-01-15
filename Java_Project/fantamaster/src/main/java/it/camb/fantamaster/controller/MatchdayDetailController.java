package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.RulesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.model.campionato.EventoData;
import it.camb.fantamaster.model.campionato.GiornataData;
import it.camb.fantamaster.model.campionato.MatchData;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.control.OverrunStyle;
import java.io.IOException;
import java.sql.SQLException;

public class MatchdayDetailController {

    @FXML private Label titleLabel;
    @FXML private VBox eventsContainer;
    
    private League currentLeague;
    private StackPane mainContentArea;
    private Rules rules;

    public static class EventoUI {
        private final String descrizione;
        private final String punteggio;
        private final double valoreNumerico;

        public EventoUI(String descrizione, String punteggio, double valoreNumerico) {
            this.descrizione = descrizione;
            this.punteggio = punteggio;
            this.valoreNumerico = valoreNumerico;
        }

        public String getDescrizione() { return descrizione; }
        public String getPunteggio() { return punteggio; }
        public double getValoreNumerico() { return valoreNumerico; }
    }

    public void initData(GiornataData giornata, League league, StackPane contentArea) {
        this.currentLeague = league;
        this.mainContentArea = contentArea;
        this.titleLabel.setText("Risultati Giornata " + giornata.giornata);
        
        try {
            RulesDAO rulesDAO = new RulesDAO(ConnectionFactory.getConnection());
            this.rules = rulesDAO.getRulesByLeagueId(league.getId());
        } catch (SQLException e) {
            this.rules = new Rules(); 
        }

        eventsContainer.getChildren().clear();

        for (MatchData match : giornata.partite) {
            VBox matchBox = new VBox(10);
            matchBox.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 12; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");
            matchBox.setMaxWidth(Double.MAX_VALUE);
            
            Label scoreHeader = new Label(match.casa + " " + match.risultato_casa + " - " + match.risultato_trasferta + " " + match.trasferta);
            scoreHeader.setStyle("-fx-text-fill: #1e40af; -fx-font-weight: bold; -fx-font-size: 15;");
            matchBox.getChildren().add(scoreHeader);

            TableView<EventoUI> table = new TableView<>();
            table.getStyleClass().add("modern-card");
            table.setSelectionModel(null);
            table.setFixedCellSize(35);
            table.setPrefHeight((match.eventi.size() * 35.0) + 38.0); 
            
            // Fondamentale per forzare le colonne a stare dentro i bordi
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

            // Colonna Evento: Prende tutto lo spazio rimanente e taglia il testo se troppo lungo
            TableColumn<EventoUI, String> colDesc = new TableColumn<>("Evento");
            colDesc.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
            colDesc.setMinWidth(100); // Permette di restringersi molto
            colDesc.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #2d3748; -fx-padding: 0 5 0 5;");
                        // Forza i puntini di sospensione se il testo è troppo lungo
                        setTextOverrun(OverrunStyle.ELLIPSIS); 
                    }
                }
            });

            // Colonna Punteggio: Priorità assoluta (Larghezza fissa e piccola)
            TableColumn<EventoUI, String> colPunt = new TableColumn<>("pt.");
            colPunt.setCellValueFactory(new PropertyValueFactory<>("punteggio"));
            // Fissiamo la larghezza per non farla mai scappare via
            colPunt.setMinWidth(60);
            colPunt.setMaxWidth(60);
            colPunt.setPrefWidth(60);
            colPunt.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        EventoUI rowData = getTableView().getItems().get(getIndex());
                        String base = "-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 13px;";
                        if (rowData.getValoreNumerico() > 0) setStyle(base + " -fx-text-fill: #16a34a;");
                        else if (rowData.getValoreNumerico() < 0) setStyle(base + " -fx-text-fill: #dc2626;");
                        else setStyle(base + " -fx-text-fill: #718096;");
                    }
                }
            });

            table.getColumns().addAll(colDesc, colPunt);

            ObservableList<EventoUI> data = FXCollections.observableArrayList();
            for (EventoData ev : match.eventi) {
                double val = getValoreAzione(ev.azione);
                data.add(new EventoUI(ev.nome + " (" + ev.azione.replace("_", " ") + ")", (val > 0 ? "+" : "") + val, val));
            }
            table.setItems(data);

            matchBox.getChildren().add(table);
            eventsContainer.getChildren().add(matchBox);
        }
    }

    private double getValoreAzione(String azione) {
        if (rules == null || azione == null) return 0.0;
        return switch (azione.toLowerCase().trim()) {
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
            Parent view = loader.load();
            SimulatedMatchdaysController ctrl = loader.getController();
            ctrl.initData(currentLeague, mainContentArea);
            mainContentArea.getChildren().setAll(view);
        } catch (IOException e) { 
            ErrorUtil.log("Errore nel tornare alla schermata precedente", e);
         }
    }
}