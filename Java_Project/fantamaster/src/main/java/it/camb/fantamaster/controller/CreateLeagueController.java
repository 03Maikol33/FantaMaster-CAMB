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
import java.sql.SQLException;
import java.time.LocalDateTime;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;

public class CreateLeagueController {

    @FXML private ImageView leagueLogoView;
    @FXML private Button pickImageButton;
    @FXML private Label imageNameLabel;
    @FXML private TextField leagueNameField;
    @FXML private TextField maxParticipantsField;
    @FXML private Label messageLabel;
    @FXML private Button createLeagueButton;

    private File selectedImageFile;

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
    private void handleCreateLeague() {
        String name = leagueNameField.getText().trim();
        String maxStr = maxParticipantsField.getText().trim();

        // 1. Validazione Input Base
        if (name.isEmpty() || maxStr.isEmpty()) {
            showError("Compila tutti i campi.");
            return;
        }

        try {
            int max = Integer.parseInt(maxStr);
            if (max <= 0) {
                showError("Inserisci un numero valido (> 0).");
                return;
            }

            // 2. Gestione Immagine (Null Safe)
            byte[] imageBytes = null;
            if (selectedImageFile != null) {
                try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                    imageBytes = fis.readAllBytes();
                } catch (IOException e) {
                    showError("Errore nella lettura dell'immagine.");
                    e.printStackTrace();
                    return;
                }
            }

            // 3. Creazione del Model (POJO)
            // Nota: Questo usa il costruttore "Nuova Lega" che abbiamo fatto nel refactoring.
            // Inserisce automaticamente l'utente corrente nella lista partecipanti in memoria.
            User currentUser = SessionUtil.getCurrentSession().getUser();
            League lega = new League(name, imageBytes, max, currentUser, LocalDateTime.now());

            // 4. Interazione con DAO (Transazionale)
            try {
                Connection conn = ConnectionFactory.getConnection();
                LeagueDAO leagueDAO = new LeagueDAO(conn);

                // Controlliamo il booleano di ritorno!
                boolean success = leagueDAO.insertLeague(lega);

                if (success) {
                    showSuccess("Lega \"" + name + "\" creata con successo!");
                    // Opzionale: Chiudi la finestra dopo il successo
                    closeWindow();
                } else {
                    showError("Errore: Impossibile creare la lega nel database.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showError("Errore di connessione al Database.");
            }

        } catch (NumberFormatException ex) {
            showError("Il numero di partecipanti deve essere un numero intero.");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) createLeagueButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
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