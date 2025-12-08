package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueListController {

    @FXML private VBox leagueContainer;

    @FXML
    public void initialize() {
        loadLeagues();
    }

    private void loadLeagues() {
        // 1. Pulisci e mostra il caricamento
        leagueContainer.getChildren().clear();
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        VBox spinnerBox = new VBox(spinner);
        spinnerBox.setAlignment(Pos.CENTER);
        spinnerBox.setPrefHeight(200); // Spazio vuoto per centrarlo
        
        leagueContainer.getChildren().add(spinnerBox);

        // 2. Esegui il lavoro pesante in un thread separato (BACKGROUND)
        CompletableFuture.supplyAsync(() -> {
            try {
                // NOTA: Qui siamo in un thread parallelo. Niente UI qui!
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                User currentUser = SessionUtil.getCurrentSession().getUser();
                return leagueDAO.getLeaguesForUser(currentUser);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.<League>emptyList();
            }
        }).thenAccept(leagues -> {
            // 3. Quando i dati sono pronti, torniamo al thread JavaFX (UI)
            Platform.runLater(() -> {
                // Rimuoviamo lo spinner
                leagueContainer.getChildren().remove(spinnerBox);

                // Popoliamo la lista
                for (League league : leagues) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueListItem.fxml"));
                        Node item = loader.load();
                        LeagueListItemController controller = loader.getController();
                        controller.setLeague(league);
                        leagueContainer.getChildren().add(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    @FXML
    private void openCreateLeague() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createLeague.fxml"));
            Parent createLeagueNode = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Crea Nuova Lega");
            stage.setScene(new Scene(createLeagueNode));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Ricarica la lista dopo la chiusura
            loadLeagues();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}