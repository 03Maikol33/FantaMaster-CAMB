package it.camb.fantamaster.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.time.LocalDateTime;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.ImageUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CreateLeagueController {

    @FXML private ImageView leagueLogoView;
    @FXML private Button pickImageButton;
    @FXML private Label imageNameLabel;
    @FXML private TextField leagueNameField;
    @FXML private TextField maxParticipantsField;
    @FXML private Label messageLabel;
    @FXML private Button createLeagueButton;
    @FXML private ComboBox<String> gameModeComboBox;

    private File selectedImageFile;

    @FXML
    public void initialize() {
        gameModeComboBox.getItems().addAll("Punti Totali", "Scontri Diretti");
    }

    @FXML
    private void hideMessageLabel() {
        messageLabel.setVisible(false);
        messageLabel.setText("");
    }

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
    private void handleGameModeSelection() {
        String selected = gameModeComboBox.getValue();
        if ("Scontri Diretti".equals(selected)) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Funzionalità Premium");
            alert.setHeaderText("Modalità riservata ai membri PRO");
            alert.setContentText("La modalità 'Scontri Diretti' è disponibile solo con l'abbonamento Platinum a 100€/mese.\n\nLa selezione verrà reimpostata a 'Punti Totali'.");
            alert.showAndWait();
            gameModeComboBox.setValue("Punti Totali");
        }
        hideMessageLabel();
    }

    @FXML
    private void handleCreateLeague() {
        String name = leagueNameField.getText().trim();
        String maxStr = maxParticipantsField.getText().trim();
        String mode = gameModeComboBox.getValue();

        if (name.isEmpty() || maxStr.isEmpty() || mode == null) {
            showError("Compila tutti i campi e seleziona una modalità.");
            return;
        }

        try {
            int max = Integer.parseInt(maxStr);
            if (max <= 0) {
                showError("Inserisci un numero valido (> 0).");
                return;
            }
            if (max % 2 != 0) {
                showError("Il numero massimo di partecipanti deve essere pari.");
                return;
            }

            byte[] imageBytes = null;
            if (selectedImageFile != null) {
                try {
                    // Leggiamo i byte originali
                    byte[] rawBytes = Files.readAllBytes(selectedImageFile.toPath());
                    
                    // Usiamo ImageUtil per comprimere (es. 300px per il logo della lega)
                    imageBytes = ImageUtil.compressImage(rawBytes, 300, 0.7f); 
                    
                } catch (IOException e) {
                    ErrorUtil.log("Errore lettura immagine lega", e);
                    showError("Errore nella lettura dell'immagine.");
                }
            }

            /*byte[] imageBytes = null;
            if (selectedImageFile != null) {
                try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                    imageBytes = fis.readAllBytes();
                } catch (IOException e) {
                
                }
            }*/

            User creator = SessionUtil.getCurrentSession().getUser();
            
            League lega = new League(
                name,
                imageBytes,
                max,
                creator,
                mode,
                LocalDateTime.now()
            );

            boolean success = false;
            
            // *** FIX: Non chiudere la connessione qui ***
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);
                
                if (leagueDAO.insertLeague(lega)) {
                    success = true;
                } else {
                    showError("Errore nella creazione della lega.");
                }
            } catch (Exception e) {
                ErrorUtil.log("Errore creazione lega", e);
                showError("Errore di connessione al database.");
            }
            
            if (success) {
                showSuccess("Lega \"" + name +"\" creata con successo!");
                handleCancel(); 
            } 

        } catch (NumberFormatException ex) {
            showError("Inserisci un numero valido.");
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) createLeagueButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setStyle("-fx-text-fill: green;");
    }
}