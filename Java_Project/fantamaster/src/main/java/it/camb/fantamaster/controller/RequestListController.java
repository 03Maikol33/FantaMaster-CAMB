package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class RequestListController {

    @FXML private VBox requestContainer;
    @FXML private Label statusLabel; 

    private League currentLeague; 

    /**
     * Imposta la lega corrente e carica le richieste di iscrizione pendenti.
     *
     * @param league la lega per cui caricare le richieste
     */
    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        loadListData();
    }

    @FXML
    private void loadListData() {
        requestContainer.getChildren().clear();
        if (statusLabel != null) statusLabel.setVisible(false);

        // Aggiungo spinner
        ProgressIndicator spinner = new ProgressIndicator();
        VBox spinnerBox = new VBox(spinner);
        spinnerBox.setAlignment(Pos.CENTER);
        spinnerBox.setPrefHeight(100);
        requestContainer.getChildren().add(spinnerBox);

        // BACKGROUND THREAD
        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                RequestDAO requestDAO = new RequestDAO(conn);
                return requestDAO.getRequestsForLeague(currentLeague);
            } catch (SQLException e) {
                ErrorUtil.log("Errore caricamento richieste per la lega", e);
                return Collections.<Request>emptyList();
            }
        }).thenAccept(requests -> {
            // UI THREAD
            Platform.runLater(() -> {
                requestContainer.getChildren().remove(spinnerBox); // Via lo spinner

                if (requests.isEmpty()) {
                    if (statusLabel != null) {
                        statusLabel.setText("Nessuna richiesta in attesa.");
                        statusLabel.setVisible(true);
                    }
                    return;
                }

                for (Request request : requests) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RequestListItem.fxml"));
                        Node item = loader.load();
                        RequestListItemController controller = loader.getController();
                        controller.setRequest(request);
                        controller.setParentController(this);
                        requestContainer.getChildren().add(item);
                    } catch (IOException e) {
                        ErrorUtil.log("Errore caricamento item della lista richieste", e);
                    }
                }
            });
        });
    }

    @FXML
    public void refreshList() {
        loadListData();
    }
}