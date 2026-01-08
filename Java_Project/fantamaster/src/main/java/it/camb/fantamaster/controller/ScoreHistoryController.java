package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO.PlayerScoreRow;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import it.camb.fantamaster.dao.CampionatoDAO;
import java.util.ArrayList;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ScoreHistoryController {

    @FXML private ComboBox<Integer> comboGiornata;
    @FXML private Label lblTotale;
    @FXML private Label lblWarning;
    @FXML private TableView<PlayerScoreRow> tableScores;
    @FXML private TableColumn<PlayerScoreRow, String> colRuolo;
    @FXML private TableColumn<PlayerScoreRow, String> colNome;
    @FXML private TableColumn<PlayerScoreRow, Double> colFantavoto;
    @FXML private TableColumn<PlayerScoreRow, String> colStato;

    private League currentLeague;
    private StackPane mainContentArea;
    private User currentUser;

    public void initData(League league, StackPane contentArea) {
        this.currentLeague = league;
        this.mainContentArea = contentArea;
        
        // Recupera utente dalla sessione
        it.camb.fantamaster.util.Session session = SessionUtil.getCurrentSession(); 
        if (session != null) {
            this.currentUser = session.getUser();
        } else {
            return; 
        }
        
        // 1. SIMULA (Importante: Se la rosa è vuota, qui non farà nulla!)
        // it.camb.fantamaster.util.DataSimulator.simulateDayForUser(currentUser, currentLeague, 1);
        
        // 2. CONFIGURA TABELLA
        setupTable();
        
        // 3. CARICA GIORNATE (E SELEZIONA L'ULTIMA)
        loadMatchdays();
    }

    private void setupTable() {
        colRuolo.setCellValueFactory(new PropertyValueFactory<>("ruolo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colFantavoto.setCellValueFactory(new PropertyValueFactory<>("fantavoto"));
        
        // Colonna Stato per mostrare "Titolare" o "Panchina" (se il voto è > 0 ma non titolare, è subentrato)
        colStato.setCellValueFactory(cellData -> {
            boolean titolare = cellData.getValue().isTitolare();
            double voto = cellData.getValue().getFantavoto();
            
            String stato;
            if (titolare) {
                stato = "Titolare";
            } else if (voto > 0) {
                stato = "Subentrato"; // Panchinaro con voto
            } else {
                stato = "Panchina";
            }
            return new javafx.beans.property.SimpleStringProperty(stato);
        });

        // Listener sul menu a tendina
        comboGiornata.setOnAction(e -> {
            Integer selectedDay = comboGiornata.getValue();
            if (selectedDay != null) {
                loadScoresForDay(selectedDay);
            }
        });
    }

    private void loadMatchdays() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            CampionatoDAO campionatoDao = new CampionatoDAO(conn);
            
            // Recuperiamo l'ultima giornata conclusa nel campionato
            int ultimaConclusa = campionatoDao.getGiornataCorrente();
            
            List<Integer> allDays = new ArrayList<>();
            // Popoliamo la lista con numeri da 1 a ultimaConclusa (o 0 se non iniziato)
            for (int i = ultimaConclusa; i >= 1; i--) {
                allDays.add(i);
            }
            
            comboGiornata.setItems(FXCollections.observableArrayList(allDays));
            
            if (!allDays.isEmpty()) {
                comboGiornata.getSelectionModel().selectFirst();
                loadScoresForDay(allDays.get(0));
            } else {
                lblWarning.setText("Il campionato non è ancora iniziato.");
                lblWarning.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScoresForDay(int giornata) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            UsersLeaguesDAO dao = new UsersLeaguesDAO(conn);
            
            // Recuperiamo i voti (se esistono)
            List<PlayerScoreRow> scores = dao.getFormationScores(currentUser.getId(), currentLeague.getId(), giornata);
            
            if (scores.isEmpty()) {
                // Caso: Nessuna formazione schierata
                tableScores.setItems(FXCollections.emptyObservableList());
                lblTotale.setText("0.0");
                lblWarning.setVisible(true);
            } else {
                // Caso: Formazione presente
                tableScores.setItems(FXCollections.observableArrayList(scores));
                double totale = scores.stream().mapToDouble(PlayerScoreRow::getFantavoto).sum();
                lblTotale.setText(String.format("%.1f", totale));
                lblWarning.setVisible(false);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*private void loadMatchdays() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            UsersLeaguesDAO dao = new UsersLeaguesDAO(conn);
            List<Integer> days = dao.getPlayedMatchdays(currentUser.getId(), currentLeague.getId());
            
            comboGiornata.setItems(FXCollections.observableArrayList(days));
            
            // Seleziona automaticamente l'ultima giornata disponibile (la più recente)
            if (!days.isEmpty()) {
                comboGiornata.getSelectionModel().selectFirst(); // Essendo ordinata DESC, la prima è l'ultima
                loadScoresForDay(days.get(0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScoresForDay(int giornata) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            UsersLeaguesDAO dao = new UsersLeaguesDAO(conn);
            List<PlayerScoreRow> scores = dao.getFormationScores(currentUser.getId(), currentLeague.getId(), giornata);
            
            tableScores.setItems(FXCollections.observableArrayList(scores));

            // Calcola il totale al volo
            double totale = scores.stream().mapToDouble(PlayerScoreRow::getFantavoto).sum();
            lblTotale.setText(String.format("%.1f", totale));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_menu.fxml"));
            Parent view = loader.load();
            StatisticsMenuController controller = loader.getController();
            controller.initData(currentLeague, mainContentArea);
            mainContentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
