package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.Player;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PlayerItemController {
    @FXML private Circle roleBadge;
    @FXML private Label nomeLabel;
    @FXML private Label squadraLabel;
    @FXML private Label ruoloLabel;
    @FXML private Label ruoloFullLabel;
    @FXML private Label numeroLabel;
    @FXML private Label nazionalitaLabel;
    @FXML private Label prezzoLabel;

    private Player player;

    public void setPlayer(Player player) {
        this.player = player;
        updateDisplay();
    }

    private void updateDisplay() {
        if (player == null) return;

        // Nome e cognome
        nomeLabel.setText(player.getNome() + " " + player.getCognome());

        // Squadra
        squadraLabel.setText(player.getSquadra());

        // Ruolo
        ruoloLabel.setText(player.getRuolo());
        ruoloFullLabel.setText(getRoleFullName(player.getRuolo()));

        // Numero
        numeroLabel.setText(String.valueOf(player.getNumero()));

        // Nazionalità
        nazionalitaLabel.setText(player.getNazionalita());

        // Prezzo
        prezzoLabel.setText(player.getPrezzo() + "€");

        // Colore badge in base al ruolo
        roleBadge.setFill(getRoleColor(player.getRuolo()));
    }

    private String getRoleFullName(String ruolo) {
        return switch (ruolo) {
            case "P" -> "Portiere";
            case "D" -> "Difensore";
            case "C" -> "Centrocampista";
            case "A" -> "Attaccante";
            default -> ruolo;
        };
    }

    private Color getRoleColor(String ruolo) {
        return switch (ruolo) {
            case "P" -> Color.web("#FF6B6B"); // Rosso
            case "D" -> Color.web("#4ECDC4"); // Turchese
            case "C" -> Color.web("#FFE66D"); // Giallo
            case "A" -> Color.web("#95E1D3"); // Verde Menta
            default -> Color.web("#667eea"); // Blu di default
        };
    }
}
