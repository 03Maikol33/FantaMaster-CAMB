package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;

public class LeagueListItemController {

    @FXML private ImageView leagueIcon;
    @FXML private Label leagueName;
    @FXML private Label creatorName;
    @FXML private Label participantCount;
    @FXML private Label adminLabel;

    private League league;

    public League getLeague() {
        return league;
    }
    public void setLeague(League league) {
        this.league = league;
        setLeagueData();
    }

    private void setLeagueData() {
        User creator = league.getCreator();
        if(creator == null) {
            throw new IllegalArgumentException("Il creatore della lega è nullo");
        }
        String leagueName = league.getName();
        String creatorName = "Creatore: " + league.getCreator().getUsername();
        String participantCount = "Partecipanti: " + league.getParticipants().size();
        if(leagueName == null || creatorName == null || participantCount == null) {
            throw new IllegalArgumentException("Uno dei dati della lega è nullo");
        }

        this.leagueName.setText(leagueName);
        this.creatorName.setText(creatorName);
        this.participantCount.setText(participantCount);
        System.out.println("League ID impostato in LeagueListItemController: " + league.getId());

        if (league.getImage() != null) {
            Image img = new Image(new ByteArrayInputStream(league.getImage()));
            leagueIcon.setImage(img);
        }else {
            // Imposta un'immagine di default se non è presente un'immagine della lega
            leagueIcon.setImage(new Image(getClass().getResourceAsStream("/images/leagueDefaultPic.png")));
        }

        if(creator.equals(SessionUtil.getCurrentSession().getUser())) {
            adminLabel.visibleProperty().set(true);
        } else {
            adminLabel.visibleProperty().set(false);
        }

    }

    @FXML
    private void handleLeagueOpening() throws Exception {
        User currentUser = SessionUtil.getCurrentSession().getUser();
        if(currentUser.equals(league.getCreator())) {
            // apri schermata lega come admin
            System.out.println("Apro schermata lega come admin: " + league.getName());
            Main.showLeagueAdminScreen(league);
        } else {
            // apri schermata lega come partecipante
            System.out.println("Apro schermata lega come partecipante: " + league.getName());
            Main.showLeagueScreen(league);
        }
    }
}
