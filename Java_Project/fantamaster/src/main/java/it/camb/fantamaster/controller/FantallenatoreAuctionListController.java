package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FantallenatoreAuctionListController { 

    @FXML private VBox fantallenatoriContainer;

    private League currentLeague;
    private User currentUser;
    private boolean isAdmin;

    // --- COSTANTI LIMITI ROSA ---
    private static final int MAX_PORTIERI = 3;
    private static final int MAX_DIFENSORI = 8;
    private static final int MAX_CENTROCAMPISTI = 8;
    private static final int MAX_ATTACCANTI = 6;
    private static final int MAX_ROSA_TOTAL = MAX_PORTIERI + MAX_DIFENSORI + MAX_CENTROCAMPISTI + MAX_ATTACCANTI;

    public void initData(League league) {
        this.currentLeague = league;
        this.currentUser = SessionUtil.getCurrentSession().getUser();
        // Verifica se l'utente corrente è l'admin della lega
        this.isAdmin = (currentUser.getId() == league.getCreator().getId());
        
        if (!league.isAuctionOpen()) {
            showAlert("Attenzione", "L'asta per questa lega è chiusa. Non dovresti essere qui!");
            // Potresti disabilitare il container o tornare indietro
            fantallenatoriContainer.setDisable(true); 
        }

        loadFantallenatori();
    }


    public void loadFantallenatori() {
        fantallenatoriContainer.getChildren().clear();

        try (Connection conn = ConnectionFactory.getConnection()) {
            UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);

            int maxPlayers = MAX_ROSA_TOTAL;

            // 1. Recupera partecipanti (Usa il metodo corretto del DAO)
            List<User> participants = ulDAO.getUsersInLeagueId(currentLeague.getId());
            
            // 2. Itera e crea le righe
            for (User participant : participants) {
                Rosa rosa = rosaDAO.getRosaByUserAndLeague(participant.getId(), currentLeague.getId());
                
                int playersCount = rosaDAO.countGiocatoriInRosa(rosa.getId());
                boolean isRosterFull = playersCount >= maxPlayers;

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fantallenatoreListItem.fxml"));
                Node node = loader.load();
                
                FantallenatoreListItemController itemController = loader.getController();

                // Passiamo 'this::handleAssignTurn' come callback
                itemController.setData(
                    participant, 
                    playersCount, 
                    maxPlayers, 
                    isRosterFull, 
                    isAdmin,
                    this::handleAssignTurn 
                );

                fantallenatoriContainer.getChildren().add(node);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile caricare la lista fantallenatori.");
        }
    }

    // Questa funzione viene chiamata quando clicchi il bottone nell'item
    private void handleAssignTurn(User targetUser) {
        if (!isAdmin) return;


        //alert di conferma
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Assegnazione");
        confirm.setHeaderText("Assegnare il turno?");
        confirm.setContentText("Vuoi dare la parola a " + targetUser.getUsername() + "?");

        if (confirm.showAndWait().orElse(null) != javafx.scene.control.ButtonType.OK) {
            return; // Se preme Annulla, esce
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            
            // Aggiorna il DB: Assegna il turno all'utente cliccato
            leagueDAO.updateTurnoAsta(currentLeague.getId(), targetUser.getId(), null);
            
            System.out.println("Turno assegnato a: " + targetUser.getUsername());
            
            // Ricarichiamo la lista (anche se idealmente il padre dovrebbe cambiare schermata ora)
            loadFantallenatori();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile assegnare il turno.");
        }
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}