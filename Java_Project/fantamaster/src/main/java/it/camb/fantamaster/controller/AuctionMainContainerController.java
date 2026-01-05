package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class AuctionMainContainerController {

    @FXML private BorderPane mainContainer;

    private Timeline pollingTimeline;
    private int currentLeagueId;
    private String currentView = ""; 

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
        // Eseguiamo il primo controllo
        runCheckTask();

        // Polling ogni 2 secondi
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            runCheckTask();
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    private boolean isPollingInProgress = false; // Aggiungi questa variabile di classe

    private void runCheckTask() {
        if (isPollingInProgress) return; // Salta se il controllo precedente non è ancora finito

        isPollingInProgress = true;
        Thread thread = new Thread(() -> {
            try {
                // NON usare try-with-resources sulla connessione se è un Singleton!
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
        // STATO 1: Turno non assegnato
        if (league.getTurnoAstaUtenteId() == null || league.getTurnoAstaUtenteId() == 0) {
            if (currentUser.getId() == league.getCreator().getId()) {
                loadView("/fxml/fantallenatoreAuctionList.fxml", "ADMIN_SELECT_TURN", league);
            } else {
                loadView("/fxml/AuctionWaitTurnSelection.fxml", "WAIT_ADMIN", league);
            }
        } 
        // STATO 2: Turno assegnato, scelta giocatore
        else if (league.getGiocatoreChiamatoId() == null || league.getGiocatoreChiamatoId() == 0) {
            if (currentUser.getId()==league.getTurnoAstaUtenteId()) {
                loadView("/fxml/AuctionProposePlayer.fxml", "PROPOSE_PLAYER", league);
            } else {
                loadView("/fxml/AuctionWaitPlayerProposal.fxml", "WAIT_PROPOSAL", league);
            }
        } 
        // STATO 3: Asta in corso
        else {
            loadView("/fxml/AuctionBiddingRoom.fxml", "BIDDING", league);
        }
    }

    private void loadView(String fxmlPath, String viewKey, League league) {
        if (currentView.equals(viewKey)) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            
            // --- PASSAGGIO DATI FONDAMENTALE ---
            Object controller = loader.getController();
            
            // Se è il controller della lista, gli passiamo la lega e chiamiamo l'init
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
            e.printStackTrace();
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
}