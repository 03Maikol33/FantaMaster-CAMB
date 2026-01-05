package it.camb.fantamaster.controller;

import java.io.ByteArrayInputStream;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LeagueListItemController {

    @FXML private ImageView leagueIcon;
    @FXML private Label leagueName;
    @FXML private Label creatorName;
    @FXML private Label participantCount;
    @FXML private HBox adminBadgeContainer;

    private League league;

    public void setLeague(League league) {
        this.league = league;
        setLeagueData();
    }

    private void setLeagueData() {
        if (league == null) return;

        leagueName.setText(league.getName() != null ? league.getName() : "Lega senza nome");

        User creator = league.getCreator();
        if (creator != null) {
            creatorName.setText("Creatore: " + creator.getUsername());
        } else {
            creatorName.setText("Creatore: Sconosciuto");
            adminBadgeContainer.setVisible(false);
        }

        int count = (league.getParticipants() != null) ? league.getParticipants().size() : 0;
        participantCount.setText("Partecipanti: " + count);

        if (league.getImage() != null && league.getImage().length > 0) {
            try {
                leagueIcon.setImage(new Image(new ByteArrayInputStream(league.getImage())));
            } catch (Exception e) {
                setDefaultImage();
            }
        } else {
            setDefaultImage();
        }

        User currentUser = SessionUtil.getCurrentSession().getUser();
        boolean isAdmin = (creator != null && creator.getId() == currentUser.getId());
        adminBadgeContainer.setVisible(isAdmin);
    }

    private void setDefaultImage() {
        try {
            leagueIcon.setImage(new Image(getClass().getResourceAsStream("/images/leagueDefaultPic.png")));
        } catch (Exception e) {
            System.err.println("Impossibile caricare immagine di default.");
        }
    }

    @FXML
    private void handleLeagueOpening() {
        try {
            User currentUser = SessionUtil.getCurrentSession().getUser();
            User creator = league.getCreator();
            if (creator != null && creator.getId() == currentUser.getId()) {
                Main.showLeagueAdminScreen(league);
            } else {
                Main.showLeagueScreen(league);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuickFormation() {
        try {
            // Apre la formazione in una finestra pop-up per non modificare LeagueAdminScreenController
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/formation.fxml"));
            Parent root = loader.load();
            
            FormationController controller = loader.getController();
            controller.setLeague(league);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Schiera Formazione - " + league.getName());
            popupStage.setScene(new Scene(root));
            popupStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}