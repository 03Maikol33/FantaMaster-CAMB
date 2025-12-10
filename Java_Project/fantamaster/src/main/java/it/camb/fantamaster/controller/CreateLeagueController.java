package it.camb.fantamaster.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView; // Import aggiunto per l'iscrizione bass
import javafx.stage.FileChooser;
import javafx.stage.Stage;




public class CreateLeagueController {

    @FXML
    private ImageView leagueLogoView;

    @FXML
    private Button pickImageButton;

    @FXML
    private Label imageNameLabel;

    @FXML
    private TextField leagueNameField;

    @FXML
    private TextField maxParticipantsField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button createLeagueButton;

    // Variabile per salvare il file immagine scelto
    private File selectedImageFile;

    // Nasconde il messaggio di errore quando l’utente digita
    @FXML
    private void hideMessageLabel() {
        messageLabel.setVisible(false);
        messageLabel.setText("");
    }

    // Gestione scelta immagine
    @FXML
    private void handlePickImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona immagine lega");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) pickImageButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            imageNameLabel.setText(file.getName());
            leagueLogoView.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleCreateLeague() {
        String name = leagueNameField.getText().trim();
        String maxStr = maxParticipantsField.getText().trim();

        if (name.isEmpty() || maxStr.isEmpty()) {
            messageLabel.setText("Compila tutti i campi.");
            return;
        }

        try {
            int max = Integer.parseInt(maxStr);
            if (max <= 0) {
                messageLabel.setText("Inserisci un numero valido (> 0).");
                return;
            }

            if (max % 2 != 0) {
                showError("Il numero massimo di partecipanti deve essere pari.");
                return;
            }

            // Gestione immagine
            byte[] imageBytes = null;
            if (selectedImageFile != null) {
                try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                    imageBytes = fis.readAllBytes();
                } catch (IOException e) {
                    System.err.println("Errore nel caricamento dell'immagine: " + e.getMessage());
                }
            }

            User creator = SessionUtil.getCurrentSession().getUser();
            
            // Nota: l'ID qui è 0 o null, verrà aggiornato dal DAO
            League lega = new League(
                name,
                imageBytes,
                max,
                creator,
                LocalDateTime.now()
            );

            boolean success = false;
            
            try (Connection conn = ConnectionFactory.getConnection()) {
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                
                // Il metodo insertLeague ora gestisce anche l'iscrizione automatica
                if (leagueDAO.insertLeague(lega)) {
                    success = true; // *** IMPORTANTE: Impostiamo il successo ***
                } else {
                    showError("Errore nella creazione della lega. Riprova.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Errore di connessione al database.");
            }
            
            if (success) {
                // Mostra messaggio di successo
                showSuccess("Lega \"" + name +"\" creata con successo!");
                // Chiudi la finestra
                handleCancel(); 
            } 
            // Non serve l'else qui perché l'errore è già gestito nel blocco try

        } catch (NumberFormatException ex) {
            showError("Inserisci un numero valido.");
        }
    }
    


            /*String name, byte[] image, int maxMembers, User creator, LocalDateTime createdAt
            League lega = new League(
            name,
            imageBytes,
            max,
            SessionUtil.getCurrentSession().getUser(),
            LocalDateTime.now());


            //crea la lega nel database
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);

                leagueDAO.insertLeague(lega);

            } catch (Exception e) {
                e.printStackTrace();
            }
            


        } catch (NumberFormatException ex) {
            messageLabel.setText("Inserisci un numero valido.");
        }
    } */


    

    // Annulla e torna indietro
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) createLeagueButton.getScene().getWindow();
        stage.close();
    }

    // Utility per mostrare errori
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    // Utility per mostrare successo
    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setStyle("-fx-text-fill: green;");
    }
}

