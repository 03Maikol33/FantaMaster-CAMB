package it.camb.fantamaster.controller;

import java.util.Collections;
import java.util.List;

import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class LeagueListController {

    @FXML private VBox leagueContainer;

    @FXML
    public void initialize() {
        // ottengo la lista delle leghe per l'utente loggato usando ConnectionFactory
        List<League> leagues;
        try (java.sql.Connection conn = ConnectionFactory.getConnection()) { // usa la mia ConnectionFactory in package Util
            UsersLeaguesDAO usersLeaguesDAO = new UsersLeaguesDAO(conn);
            leagues = usersLeaguesDAO.getLeaguesForUser(SessionUtil.getCurrentSession().getUser());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            leagues = Collections.emptyList();
        }

        for (League league : leagues) {
            System.out.println("Caricamento lega: " + league);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueListItem.fxml"));
                Node item = loader.load();

                LeagueListItemController controller = loader.getController();
                controller.setLeague(league);
                //controller.setLeagueData();

                leagueContainer.getChildren().add(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void openCreateLeague() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createLeague.fxml"));
            Node createLeagueNode = loader.load();

            // Apre la finestra di creazione lega
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Crea Nuova Lega");
            stage.setScene(new javafx.scene.Scene((javafx.scene.Parent) createLeagueNode));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();  
        }
    }
}
