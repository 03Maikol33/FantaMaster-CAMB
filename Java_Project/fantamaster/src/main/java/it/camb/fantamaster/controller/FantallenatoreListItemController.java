package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

public class FantallenatoreListItemController {

    @FXML private HBox itemContainer;
    @FXML private Circle avatarCircle;
    @FXML private Circle statusIndicator;
    @FXML private Label usernameLabel;
    @FXML private Label slotsLabel;
    @FXML private Button assignTurnButton;

    private User user;
    
    // Consumer è l'interfaccia per la callback (riceve un User e non ritorna nulla)
    private Consumer<User> onAssignTurnCallback; 

    /**
     * Imposta i dati dell'elemento lista per un fantallenatore e aggiorna la UI.
     *
     * @param user l'utente rappresentato dalla riga
     * @param playersCount numero di giocatori già acquistati
     * @param maxPlayers numero massimo di giocatori consentiti
     * @param isRosterFull true se la rosa è completa, altrimenti false
     * @param isAdmin true se l'utente corrente è admin della lega
     * @param onAssignAction callback da eseguire quando si assegna il turno all'utente
     */
    public void setData(User user, int playersCount, int maxPlayers, boolean isRosterFull, boolean isAdmin, Consumer<User> onAssignAction) {
        this.user = user;
        this.onAssignTurnCallback = onAssignAction;

        usernameLabel.setText(user.getUsername());
        
        // Gestione Avatar
        if (user.getAvatar() != null && user.getAvatar().length > 0) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(user.getAvatar());
                Image image = new Image(bis);
                avatarCircle.setFill(new ImagePattern(image));
            } catch (Exception e) {
                avatarCircle.setFill(Color.web("#cbd5e0"));
            }
        } else {
            avatarCircle.setFill(Color.web("#cbd5e0"));
        }

        slotsLabel.setText(playersCount + "/" + maxPlayers);

        // LOGICA UI
        if (isRosterFull) {
            // Rosa Piena: Disabilitato
            itemContainer.setOpacity(0.5);
            assignTurnButton.setVisible(false);
            
            slotsLabel.setStyle("-fx-text-fill: #e53e3e; -fx-background-color: #fff5f5; -fx-background-radius: 10; -fx-padding: 4 8;");
            statusIndicator.setFill(Color.web("#e53e3e")); // Rosso
        } else {
            // Disponibile
            itemContainer.setOpacity(1.0);
            
            slotsLabel.setStyle("-fx-text-fill: #718096; -fx-background-color: #edf2f7; -fx-padding: 4 8; -fx-background-radius: 10;");
            statusIndicator.setFill(Color.web("#48bb78")); // Verde

            // Il bottone appare SOLO se sono Admin
            if (isAdmin) {
                assignTurnButton.setVisible(true);
                assignTurnButton.setDisable(false);
            } else {
                assignTurnButton.setVisible(false);
            }
        }
    }

    // Metodo collegato al bottone nell'FXML (o inizializzato qui)
    @FXML
    /**
     * Inizializza i listener della riga e collega l'azione del bottone di assegnazione turno.
     */
    public void initialize() {
        assignTurnButton.setOnAction(e -> {
            // Quando clicco, se la callback esiste, la eseguo passando l'utente di questa riga
            if (onAssignTurnCallback != null && user != null) {
                onAssignTurnCallback.accept(user);
            }
        });
    }
}