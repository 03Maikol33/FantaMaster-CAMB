package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;


public class RequestListController {
    private League currentLeague; // tiene il riferimento alla lega attualmente aperta
    @FXML private VBox requestContainer;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        loadListData();
    }

    @FXML
    private void loadListData() {
        // ottengo la lista delle richieste per la lega aperta
        List<Request> requests;
        try (Connection conn = ConnectionFactory.getConnection()) { // usa la mia ConnectionFactory in package Util
            RequestDAO requestDAO = new RequestDAO(conn);
            requests = requestDAO.getRequestsForLeague(currentLeague);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            requests = Collections.emptyList();
        }

        for (Request request : requests) {
            System.out.println("Caricamento richiesta: " + request);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RequestListItem.fxml"));
                Node item = loader.load();

                RequestListItemController controller = loader.getController();
                controller.setRequestData(request);
                controller.setRequest(request);
                controller.setParentController(this);

                requestContainer.getChildren().add(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void refreshList() {
        requestContainer.getChildren().clear();
        loadListData();
    }
}
