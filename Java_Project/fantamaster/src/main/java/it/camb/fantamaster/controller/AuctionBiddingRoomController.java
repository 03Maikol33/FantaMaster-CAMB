package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.dao.PlayerDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionBiddingRoomController {

    @FXML private Label nomeGiocatoreLabel;
    @FXML private Label ruoloLabel;
    @FXML private Label squadraLabel;
    @FXML private Label prezzoLabel;

    private League currentLeague;

    public void initData(League league) {
        this.currentLeague = league;
        
        // Recuperiamo i dati del giocatore chiamato dal JSON usando l'ID salvato nella lega
        if (league.getGiocatoreChiamatoId() != null) {
            PlayerDAO playerDAO = new PlayerDAO();
            Player p = playerDAO.getPlayerById(league.getGiocatoreChiamatoId());
            
            if (p != null) {
                nomeGiocatoreLabel.setText(p.getNome() + " " + p.getCognome());
                ruoloLabel.setText("Ruolo: " + p.getRuolo());
                squadraLabel.setText(p.getSquadra()); // Imposta la squadra dal JSON
                prezzoLabel.setText(String.valueOf(p.getPrezzo()));
            }
        }
    }
}