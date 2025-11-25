package it.camb.fantamaster.controller;

import java.sql.Connection;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class LeagueAdminSettingsController {
    @FXML private Button closeRegistrationsButton;
    @FXML private Label closeRegistrationsWarningLabel;

    private League currentLeague;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        loadLeagueAdminSettings();
    }

    private void loadLeagueAdminSettings() {
        if (currentLeague != null) {
            // Aggiorna lo stato del pulsante in base allo stato delle iscrizioni della lega
            if(currentLeague.isRegistrationsClosed()) {
                closeRegistrationsButton.setDisable(true);
                closeRegistrationsButton.getStyleClass().add("disabled-action-color");
                closeRegistrationsWarningLabel.setText("Le iscrizioni sono già chiuse per questa lega.");
                closeRegistrationsWarningLabel.getStyleClass().add("info-action-color");
            } else {
                if(currentLeague.getParticipants().size() % 2 != 0) {
                    closeRegistrationsButton.setDisable(true);
                    closeRegistrationsButton.getStyleClass().add("disabled-action-color");
                    closeRegistrationsWarningLabel.setText("Puoi chiudere le iscrizioni solo se il numero di partecipanti è pari.");
                    closeRegistrationsWarningLabel.getStyleClass().add("info-action-color");
                }else{
                    closeRegistrationsButton.setDisable(false);
                    closeRegistrationsButton.getStyleClass().removeAll("disabled-action-color");
                    closeRegistrationsButton.getStyleClass().add("irreversible-action-color");
                    closeRegistrationsWarningLabel.setText("Attenzione: questa azione è irreversibile e impedirà a nuovi utenti di iscriversi alla lega.");
                    closeRegistrationsWarningLabel.getStyleClass().removeAll("info-action-color");
                    closeRegistrationsWarningLabel.getStyleClass().add("irreversible-action-color");
                }
            }
        }
    }

    public void handleCloseRegistrations() {
        if (currentLeague != null && currentLeague.getParticipants().size() % 2 == 0) { // Controlla se il numero di partecipanti è pari
            try{
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                if(leagueDAO.closeRegistrations(currentLeague.getId())) {
                    currentLeague.setRegistrationsClosed(true);
                    System.out.println("Iscrizioni chiuse con successo per la lega: " + currentLeague.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Nessuna lega selezionata per chiudere le iscrizioni.");
        }
    }
}