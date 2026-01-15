package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public class AuctionMainContainerController {

    @FXML private BorderPane mainContainer;

    private Timeline pollingTimeline;
    private int currentLeagueId;
    private String currentView = ""; 

    private Integer lastGiocatoreId = null;
    private boolean isPollingInProgress = false;

    public void initData(int leagueId) {
        this.currentLeagueId = leagueId;
        
        mainContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stopPolling();
            }
        });

        startPolling();
    }

    private void startPolling() {
        runCheckTask();

        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            runCheckTask();
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    private void runCheckTask() {
        if (isPollingInProgress) return; 

        isPollingInProgress = true;
        Thread thread = new Thread(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection(); 
                
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                League league = leagueDAO.getLeagueById(currentLeagueId);
                User currentUser = SessionUtil.getCurrentSession().getUser();

                if (league != null) {
                    Platform.runLater(() -> updateAuctionView(league, currentUser));
                }
            } catch (Exception e) {
                System.err.println("Errore nel polling: " + e.getMessage());
            } finally {
                isPollingInProgress = false;
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void updateAuctionView(League league, User currentUser) {
        // --- LOGICA DI SINCRONIZZAZIONE NOTIFICA ---
        Integer currentGiocatoreId = league.getGiocatoreChiamatoId();

        if (this.lastGiocatoreId != null && (currentGiocatoreId == null || currentGiocatoreId == 0)) {
            System.out.println("[Auction] Rilevata fine asta per giocatore ID: " + lastGiocatoreId);
            mostraRisultatoAsta(league.getId());
        }

        this.lastGiocatoreId = currentGiocatoreId;

        // --- GESTIONE ASTA CHIUSA (FIX LOOP ALERT) ---
        // Se l'asta è chiusa, mostriamo la vista di attesa ed usciamo
        if (!league.isAuctionOpen()) {
            loadView("/fxml/AuctionWaitTurnSelection.fxml", "WAIT_ADMIN", league);
            return;
        }

        // --- LOGICA DI SWITCH VISTE ---
        if (league.getTurnoAstaUtenteId() == null || league.getTurnoAstaUtenteId() == 0) {
            if (currentUser.getId() == league.getCreator().getId()) {
                loadView("/fxml/fantallenatoreAuctionList.fxml", "ADMIN_SELECT_TURN", league);
            } else {
                loadView("/fxml/AuctionWaitTurnSelection.fxml", "WAIT_ADMIN", league);
            }
        } 
        else if (currentGiocatoreId == null || currentGiocatoreId == 0) {
            if (currentUser.getId() == league.getTurnoAstaUtenteId()) {
                loadView("/fxml/AuctionProposePlayer.fxml", "PROPOSE_PLAYER", league);
            } else {
                loadView("/fxml/AuctionWaitPlayerProposal.fxml", "WAIT_PROPOSAL", league);
            }
        } 
        else {
            loadView("/fxml/AuctionBiddingRoom.fxml", "BIDDING", league);
        }
    }

    private void loadView(String fxmlPath, String viewKey, League league) {
        if (currentView.equals(viewKey)) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            
            Object controller = loader.getController();
            
            if (controller instanceof FantallenatoreAuctionListController) {
                ((FantallenatoreAuctionListController) controller).initData(league);
                ((FantallenatoreAuctionListController) controller).setParentContainer(this);
            }
             
            if (controller instanceof AuctionProposePlayerController) {
                ((AuctionProposePlayerController) controller).initData(league);
                ((AuctionProposePlayerController) controller).setParentContainer(this);
            }

            if (controller instanceof AuctionBiddingRoomController) {
                ((AuctionBiddingRoomController) controller).initData(league);
            }
            
            mainContainer.setCenter(node);
            currentView = viewKey;
            System.out.println("[Auction] Vista cambiata in: " + viewKey);
            
        } catch (IOException e) {
            ErrorUtil.log("Errore caricamento vista asta: " + fxmlPath, e);
        }
    }

    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            System.out.println("Polling rimosso.");
        }
    }

    public void forceRefresh() {
        System.out.println("[Auction] Refresh forzato richiesto...");
        runCheckTask();
    }

    private void mostraRisultatoAsta(int leagueId) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            it.camb.fantamaster.dao.AuctionDAO auctionDAO = new it.camb.fantamaster.dao.AuctionDAO(conn);
            String risultato = auctionDAO.getUltimoRisultatoAsta(leagueId);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ASTA CONCLUSA");
            alert.setHeaderText("Il giocatore è stato assegnato!");
            alert.setContentText("Esito: " + risultato);
            alert.show(); 

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dell'ultimo risultato asta: " + e.getMessage());
        }
    }
}