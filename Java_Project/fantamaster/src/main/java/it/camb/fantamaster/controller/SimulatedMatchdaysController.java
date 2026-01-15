package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.CampionatoDAO;
import it.camb.fantamaster.util.CampionatoUtil;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.campionato.GiornataData;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SimulatedMatchdaysController {

    @FXML private VBox matchdayContainer;
    private League league;
    private StackPane mainContentArea; // Aggiunto per la navigazione interna

    public void initData(League league, StackPane contentArea) {
        this.league = league;
        this.mainContentArea = contentArea;
        loadMatchdays();
    }

    public void setLeague(League league) {
        this.league = league;
    }

    private void loadMatchdays() {
        matchdayContainer.getChildren().clear();
        try {
            CampionatoDAO campionatoDAO = new CampionatoDAO(ConnectionFactory.getConnection());
            int giornataCorrente = campionatoDAO.getGiornataCorrente();

            CampionatoUtil.load("/api/campionato.json");
            List<GiornataData> giornateVisibili = CampionatoUtil.getGiornate().stream()
                .filter(g -> g.giornata <= giornataCorrente)
                .toList();

            if (giornateVisibili.isEmpty()) {
                Label emptyLabel = new Label("Nessuna giornata ancora calcolata.");
                emptyLabel.getStyleClass().add("hint-text");
                matchdayContainer.getChildren().add(emptyLabel);
                return;
            }

            for (GiornataData g : giornateVisibili) {
                VBox card = new VBox();
                card.getStyleClass().add("modern-card");
                
                Label title = new Label("GIORNATA " + g.giornata);
                title.getStyleClass().add("card-title");
                
                card.getChildren().add(title);
                card.setOnMouseClicked(e -> showDetail(g));
                matchdayContainer.getChildren().add(card);
            }
        } catch (SQLException e) { 
            ErrorUtil.log("Errore caricamento giornate simulate", e);
         }
    }

    private void showDetail(GiornataData giornata) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matchday_detail.fxml"));
            Parent view = loader.load();
            MatchdayDetailController ctrl = loader.getController();
            // Passiamo anche qui il riferimento all'area contenuti
            ctrl.initData(giornata, league, mainContentArea);
            mainContentArea.getChildren().setAll(view);
        } catch (IOException e) { 
            ErrorUtil.log("Errore caricamento dettaglio giornata", e);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_menu.fxml"));
            Parent view = loader.load();
            StatisticsMenuController ctrl = loader.getController();
            ctrl.initData(league, mainContentArea);
            mainContentArea.getChildren().setAll(view);
        } catch (IOException e) { 
            ErrorUtil.log("Errore caricamento schermata statistiche", e);
        }
    }
}