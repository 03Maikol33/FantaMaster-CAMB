package it.camb.fantamaster.controller;

import it.camb.fantamaster.util.CampionatoUtil;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.campionato.GiornataData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

public class SimulatedMatchdaysController {

    @FXML private VBox matchdayContainer;
    private League league;

    public void setLeague(League league) {
        this.league = league;
        loadMatchdays();
    }

    private void loadMatchdays() {
        matchdayContainer.getChildren().clear();
        CampionatoUtil.load("/api/campionato.json");
        List<GiornataData> giornate = CampionatoUtil.getGiornate();
        
        if (giornate != null) {
            for (GiornataData g : giornate) {
                Button btn = new Button("GIORNATA " + g.giornata);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setPrefHeight(45);
                btn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
                btn.setOnAction(e -> showDetail(g));
                matchdayContainer.getChildren().add(btn);
            }
        }
    }

    private void showDetail(GiornataData giornata) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matchday_detail.fxml"));
            Parent root = loader.load();
            
            MatchdayDetailController ctrl = loader.getController();
            ctrl.setData(giornata, league);

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