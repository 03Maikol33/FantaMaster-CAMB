package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.League;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class StatisticsMenuController {

    private League currentLeague;
    private StackPane mainContentArea; // Riferimento all'area centrale per cambiare view

    public void initData(League league, StackPane contentArea) {
        this.currentLeague = league;
        this.mainContentArea = contentArea;
    }

    @FXML
    private void goToGeneralRanking() {
        System.out.println("Navigazione verso: Classifica Generale");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ranking_general.fxml"));
            Parent view = loader.load();

            RankingGeneralController controller = loader.getController();
            // Passiamo la lega e l'area contenuti per mantenere il contesto
            controller.initData(currentLeague, mainContentArea);

            mainContentArea.getChildren().setAll(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricamento di ranking_general.fxml");
        }
    }
@FXML
    private void goToScoreHistory() {
        System.out.println("Navigazione verso: Storico Punteggi");
        try {
            // 1. CARICA IL FILE FXML GIUSTO (Quello nuovo che abbiamo creato)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/score_history.fxml"));
            Parent view = loader.load();

            // 2. USA IL CONTROLLER GIUSTO (Quello che contiene initData)
            ScoreHistoryController controller = loader.getController();
            
            // 3. ORA initData FUNZIONERÀ
            controller.initData(currentLeague, mainContentArea);

            mainContentArea.getChildren().setAll(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricamento di score_history.fxml");
        }
    
    }


    @FXML
    private void goToMatchdayResults() {
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/simulated_matchdays.fxml"));
            Parent view = loader.load();

            SimulatedMatchdaysController controller = loader.getController();
            controller.setLeague(currentLeague); // Passiamo la lega come richiesto dal controller esistente

            // Sostituiamo la vista nel contenitore principale
            mainContentArea.getChildren().setAll(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricamento di simulated_matchdays.fxml");
        }
    }
}
