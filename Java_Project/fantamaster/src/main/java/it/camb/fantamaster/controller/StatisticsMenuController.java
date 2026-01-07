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
        // TODO: Implementare caricamento classifica_generale.fxml
        // loadSubView("/fxml/ranking_general.fxml");
    }

    @FXML
    private void goToScoreHistory() {
        System.out.println("Navigazione verso: Storico Punteggi");
        // TODO: Implementare caricamento storico_punteggi.fxml
    }

    @FXML
    private void goToLastMatchdayScores() {
        System.out.println("Navigazione verso: Punteggi Ultima Giornata");
        // TODO: Implementare caricamento punteggi_ultima.fxml
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
