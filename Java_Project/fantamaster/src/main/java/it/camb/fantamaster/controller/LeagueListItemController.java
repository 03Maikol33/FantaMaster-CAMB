package it.camb.fantamaster.controller;

import java.io.ByteArrayInputStream;
import java.sql.Connection;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.CampionatoDAO;
import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        // 1. Creiamo un popup di caricamento veloce
        Stage loadingStage = createLoadingPopup();
        Label statusLabel = (Label) loadingStage.getScene().getRoot().lookup("#statusLabel");
        loadingStage.show();

        // 2. Definiamo il Task in background
        javafx.concurrent.Task<Void> syncTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Inizializzazione...");
                Connection conn = ConnectionFactory.getConnection();
                User currentUser = SessionUtil.getCurrentSession().getUser();

                // A. Fix Rosa (Lazy Creation)
                updateMessage("Verifica integrità rosa...");
                RosaDAO rosaDAO = new RosaDAO(conn);
                if (rosaDAO.getRosaByUserAndLeague(currentUser.getId(), league.getId()) == null) {
                    UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(conn);
                    int ulId = ulDAO.getExistingSubscriptionId(currentUser.getId(), league.getId());
                    if (ulId != -1) rosaDAO.createDefaultRosa(ulId);
                }

                // B. Sincronizzazione con messaggi dinamici
                CampionatoDAO campionatoDAO = new CampionatoDAO(conn);
                // Passiamo il metodo updateMessage del Task al DAO come Consumer
                campionatoDAO.sincronizzaPunteggiLega(league.getId(), this::updateMessage);

                return null;
            }
        };

        // 3. Colleghiamo il messaggio del Task alla Label della UI
        statusLabel.textProperty().bind(syncTask.messageProperty());

        // 4. Cosa fare al termine (Successo)
        syncTask.setOnSucceeded(e -> {
            loadingStage.close();
            try {
                User currentUser = SessionUtil.getCurrentSession().getUser();
                User creator = league.getCreator();
                if (creator != null && creator.getId() == currentUser.getId()) {
                    Main.showLeagueAdminScreen(league);
                } else {
                    Main.showLeagueScreen(league);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // 5. Gestione Errori
        syncTask.setOnFailed(e -> {
            loadingStage.close();
            new Alert(Alert.AlertType.ERROR, "Errore sincronizzazione: " + syncTask.getException().getMessage()).show();
        });

        // 6. Avvio del thread
        new Thread(syncTask).start();
    }

    /**
     * Crea una semplice finestra di attesa senza decorazioni.
     */
    private Stage createLoadingPopup() {
        Stage stage = new Stage(javafx.stage.StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        
        Label lbl = new Label("Sincronizzazione in corso...");
        lbl.setId("statusLabel");
        lbl.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(50, 50);
        
        VBox root = new VBox(15, pi, lbl);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-border-color: #2b6cb0; -fx-border-width: 2; -fx-padding: 20;");
        
        stage.setScene(new Scene(root, 250, 150));
        return stage;
    }

    @FXML
    private void handleQuickFormation() {
        try {
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

    @FXML
    private void handleShowResults() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/simulated_matchdays.fxml"));
            Parent root = loader.load();
            
            SimulatedMatchdaysController controller = loader.getController();
            controller.setLeague(league);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Risultati Giornate - " + league.getName());
            popupStage.setScene(new Scene(root));
            popupStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}