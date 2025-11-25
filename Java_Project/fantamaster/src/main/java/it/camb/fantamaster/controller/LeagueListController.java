package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;

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
}
