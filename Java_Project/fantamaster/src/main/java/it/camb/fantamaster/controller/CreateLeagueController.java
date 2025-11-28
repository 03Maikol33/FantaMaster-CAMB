package it.camb.fantamaster.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;

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
            messageLabel.setText("Lega \"" + name + "\" creata con " + max + " partecipanti!");

            //creo la lega nel sistema
            
            byte[] imageBytes = null;
            try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                imageBytes = fis.readAllBytes(); // disponibile da Java 9
            } catch (IOException e) {
                e.printStackTrace();
            }
            //String name, byte[] image, int maxMembers, User creator, LocalDateTime createdAt
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
    }

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

