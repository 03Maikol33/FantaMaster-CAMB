package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.CampionatoDAO;
import it.camb.fantamaster.util.CampionatoUtil;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.campionato.GiornataData;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SimulatedMatchdaysController {

    @FXML private VBox matchdayContainer;
    private League league;

    public void setLeague(League league) {
        this.league = league;
        loadMatchdays();
    }

    private void loadMatchdays() {
        matchdayContainer.getChildren().clear();
        
        try {
            CampionatoDAO campionatoDAO = new CampionatoDAO(ConnectionFactory.getConnection());
            int giornataCorrente = campionatoDAO.getGiornataCorrente();

            CampionatoUtil.load("/api/campionato.json");
            List<GiornataData> tutteLeGiornate = CampionatoUtil.getGiornate();
            
            // Filtro: mostriamo solo le giornate sbloccate nel DB
            List<GiornataData> giornateVisibili = tutteLeGiornate.stream()
                .filter(g -> g.giornata <= giornataCorrente)
                .collect(Collectors.toList());

            if (giornateVisibili.isEmpty()) {
                Label emptyLabel = new Label("Il campionato non è ancora iniziato.");
                emptyLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-style: italic;");
                matchdayContainer.getChildren().add(emptyLabel);
                return;
            }

            for (GiornataData g : giornateVisibili) {
                Button btn = new Button("GIORNATA " + g.giornata);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setPrefHeight(45);
                btn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
                btn.setOnAction(e -> showDetail(g));
                matchdayContainer.getChildren().add(btn);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showDetail(GiornataData giornata) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matchday_detail.fxml"));
            Parent root = loader.load();
            
            MatchdayDetailController ctrl = loader.getController();
            ctrl.setData(giornata, league);

            // Cambia la radice della scena del popup corrente
            matchdayContainer.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        ((Stage) matchdayContainer.getScene().getWindow()).close();
    }
}