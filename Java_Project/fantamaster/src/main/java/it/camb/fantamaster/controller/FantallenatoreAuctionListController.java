package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

public class FantallenatoreAuctionListController { 

    @FXML private VBox fantallenatoriContainer;

    private League currentLeague;
    private User currentUser;
    private boolean isAdmin;

    private AuctionMainContainerController parentContainer;

    public void setParentContainer(AuctionMainContainerController parentContainer) {
        this.parentContainer = parentContainer;
    }

    private static final int MAX_ROSA_TOTAL = 25; // Semplificato per brevità

    public void initData(League league) {
        this.currentLeague = league;
        this.currentUser = SessionUtil.getCurrentSession().getUser();
        this.isAdmin = (currentUser.getId() == league.getCreator().getId());
        
        // RIMOSSO showAlert() qui per evitare il loop infinito causato dal polling
        if (!league.isAuctionOpen()) {
            fantallenatoriContainer.setDisable(true); 
        }

        loadFantallenatori();
    }

    public void loadFantallenatori() {
        fantallenatoriContainer.getChildren().clear();

        // Usiamo la connessione unica senza chiuderla (se Singleton) 
        // o con il try-with-resources se preferisci, basta che sia coerente con il resto del progetto
        try {
            Connection conn = ConnectionFactory.getConnection();
            UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);

            int maxPlayers = MAX_ROSA_TOTAL;

            // 1. Recupera tutti i partecipanti della lega
            List<User> participants = ulDAO.getUsersInLeagueId(currentLeague.getId());
            
            for (User participant : participants) {
                // 2. Cerchiamo la rosa nel DB
                Rosa rosa = rosaDAO.getRosaByUserAndLeague(participant.getId(), currentLeague.getId());
                
                // --- LOGICA DI CORREZIONE (Lazy Creation) ---
                // Se la rosa non esiste per questo partecipante, la creiamo al volo
                if (rosa == null) {
                    System.out.println("[Fix] Rosa mancante per " + participant.getUsername() + ". Creazione in corso...");
                    
                    // Recuperiamo l'ID del legame utenti_leghe
                    int ulId = ulDAO.getExistingSubscriptionId(participant.getId(), currentLeague.getId());
                    
                    if (ulId != -1) {
                        // Creiamo il record nel DB
                        rosaDAO.createDefaultRosa(ulId);
                        // Ricarichiamo l'oggetto rosa (ora non sarà più null)
                        rosa = rosaDAO.getRosaByUserAndLeague(participant.getId(), currentLeague.getId());
                    }
                }

                // 3. Ora procediamo con i calcoli (ora rosa.getId() è sicuro)
                if (rosa != null) {
                    int playersCount = rosaDAO.countGiocatoriInRosa(rosa.getId());
                    boolean isRosterFull = playersCount >= maxPlayers;

                    // Carichiamo l'elemento grafico
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fantallenatoreListItem.fxml"));
                    Node node = loader.load();
                    
                    FantallenatoreListItemController itemController = loader.getController();

                    // Passiamo i dati al controller della riga
                    itemController.setData(
                        participant, 
                        playersCount, 
                        maxPlayers, 
                        isRosterFull, 
                        isAdmin,
                        this::handleAssignTurn 
                    );

                    fantallenatoriContainer.getChildren().add(node);
                } else {
                    System.err.println("Errore critico: Impossibile creare rosa per " + participant.getUsername());
                }
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile caricare la lista fantallenatori.");
        }
    }

/*
    public void loadFantallenatori() {
        fantallenatoriContainer.getChildren().clear();

        try (Connection conn = ConnectionFactory.getConnection()) {
            UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);

            List<User> participants = ulDAO.getUsersInLeagueId(currentLeague.getId());
            
            for (User participant : participants) {
                Rosa rosa = rosaDAO.getRosaByUserAndLeague(participant.getId(), currentLeague.getId());
                
                // FIX NULL POINTER: Se la rosa non esiste nel DB, inizializziamo a 0
                int playersCount = 0;
                if (rosa != null) {
                    playersCount = rosaDAO.countGiocatoriInRosa(rosa.getId());
                }
                
                boolean isRosterFull = playersCount >= MAX_ROSA_TOTAL;

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fantallenatoreListItem.fxml"));
                Node node = loader.load();
                
                FantallenatoreListItemController itemController = loader.getController();
                itemController.setData(
                    participant, 
                    playersCount, 
                    MAX_ROSA_TOTAL, 
                    isRosterFull, 
                    isAdmin,
                    this::handleAssignTurn 
                );

                fantallenatoriContainer.getChildren().add(node);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }*/

    private void handleAssignTurn(User targetUser) {
        if (!isAdmin) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma");
        confirm.setHeaderText("Dare la parola a " + targetUser.getUsername() + "?");

        if (confirm.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                new LeagueDAO(conn).updateTurnoAsta(currentLeague.getId(), targetUser.getId(), null);
                loadFantallenatori();
                if (parentContainer != null) parentContainer.forceRefresh();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}