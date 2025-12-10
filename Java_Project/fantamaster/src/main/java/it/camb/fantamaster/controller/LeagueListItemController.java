package it.camb.fantamaster.controller;

import java.io.ByteArrayInputStream;

import java.io.ByteArrayInputStream;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;

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

        // 1. Gestione Nome
        leagueName.setText(league.getName() != null ? league.getName() : "Lega senza nome");

        // 2. Gestione Creatore (Difensiva)
        User creator = league.getCreator();
        if (creator != null) {
            creatorName.setText("Creatore: " + creator.getUsername());
        } else {
            creatorName.setText("Creatore: Sconosciuto");
            // Se non c'è creatore, non possiamo confrontarlo per il badge admin
            adminBadgeContainer.setVisible(false);
            return; 
        }

        // 3. Gestione Partecipanti (Null Safe)
        // Grazie al refactoring del Model, getParticipants() non dovrebbe mai essere null,
        // ma un controllo in più non guasta mai nella UI.
        int count = (league.getParticipants() != null) ? league.getParticipants().size() : 0;
        participantCount.setText("Partecipanti: " + count);

        // 4. Gestione Immagine
        if (league.getImage() != null && league.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(league.getImage()));
                leagueIcon.setImage(img);
            } catch (Exception e) {
                // Se l'immagine è corrotta, usa quella di default
                setDefaultImage();
            }
        } else {
            setDefaultImage();
        }

        // 5. Gestione Badge Admin
        // Qui sfruttiamo il metodo equals() di User che abbiamo testato
        User currentUser = SessionUtil.getCurrentSession().getUser();
        boolean isAdmin = creator.equals(currentUser);
        
        // Usa setVisible(boolean) invece di visibleProperty().set(...) -> è più idiomatico
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

            // Controllo null-safe
            if (creator != null && creator.equals(currentUser)) {
                System.out.println("Apro schermata lega come admin: " + league.getName());
                Main.showLeagueAdminScreen(league);
            } else {
                System.out.println("Apro schermata lega come partecipante: " + league.getName());
                Main.showLeagueScreen(league);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Errore nell'apertura della lega.");
        }
    }
}
// Fix conflitti definitivo