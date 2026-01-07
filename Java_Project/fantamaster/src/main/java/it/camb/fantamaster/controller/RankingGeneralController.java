package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO.UserRankingRow;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class RankingGeneralController {

    @FXML private TableView<UserRankingRow> rankingTable;
    @FXML private TableColumn<UserRankingRow, Integer> colPosizione;
    @FXML private TableColumn<UserRankingRow, String> colFantallenatore;
    @FXML private TableColumn<UserRankingRow, Double> colPunti;

    private League currentLeague;
    private StackPane mainContentArea;

    public void initData(League league, StackPane contentArea) {
        this.currentLeague = league;
        this.mainContentArea = contentArea;
        loadRankingData();
    }

    @FXML
    public void initialize() {
        // Collega le colonne agli attributi della classe UserRankingRow
        colPosizione.setCellValueFactory(new PropertyValueFactory<>("posizione"));
        // Puoi scegliere se mostrare 'username' o 'nomeSquadra'. Qui mostro il nome squadra e username
        colFantallenatore.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getNomeSquadra() + " (" + cellData.getValue().getUsername() + ")"
            )
        );
        colPunti.setCellValueFactory(new PropertyValueFactory<>("punteggio"));
    }

    private void loadRankingData() {
        try (Connection conn = ConnectionFactory.getConnection()) {
            UsersLeaguesDAO dao = new UsersLeaguesDAO(conn);
            
            // 1. (Opzionale) Ricalcola i punteggi per essere sicuri siano aggiornati
            // dao.updateLeagueRanking(currentLeague.getId()); 

            // 2. Ottieni la classifica ordinata
            List<UserRankingRow> ranking = dao.getLeagueRanking(currentLeague.getId());
            
            // 3. Converti in ObservableList per JavaFX
            ObservableList<UserRankingRow> data = FXCollections.observableArrayList(ranking);
            rankingTable.setItems(data);

        } catch (SQLException e) {
            e.printStackTrace();
            // Qui potresti mostrare un Alert di errore
        }
    }

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