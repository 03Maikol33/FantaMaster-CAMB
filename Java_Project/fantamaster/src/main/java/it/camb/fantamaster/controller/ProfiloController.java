package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.ImageUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.Arrays;

public class ProfiloController {

    @FXML private ImageView avatarImageView;
    @FXML private TextField usernameField;
    @FXML private Label emailLabel; // Cambiato da TextField a Label [cite: 1]

    private User currentUser;
    private byte[] selectedAvatarBytes;

    @FXML
    public void initialize() {
        // Recuperiamo l'ID dell'utente dalla sessione locale
        User sessionUser = SessionUtil.getCurrentSession().getUser(); 
        
        if (sessionUser != null) {
            try {
                Connection conn = ConnectionFactory.getConnection();
                UserDAO dao = new UserDAO(conn);
                
                // Cerchiamo l'utente nel DB per avere l'avatar più recente
                this.currentUser = dao.findById(sessionUser.getId());
                
                if (currentUser != null) {
                    usernameField.setText(currentUser.getUsername());
                    emailLabel.setText(currentUser.getEmail()); // Impostiamo la Label [cite: 1]
                    
                    if (currentUser.getAvatar() != null) {
                        avatarImageView.setImage(new Image(new ByteArrayInputStream(currentUser.getAvatar())));
                        selectedAvatarBytes = currentUser.getAvatar();
                    }
                }
            } catch (Exception e) {
                ErrorUtil.log("Errore nel caricamento del profilo utente", e);
                // Fallback nel caso il DB non risponda
                this.currentUser = sessionUser;
                usernameField.setText(currentUser.getUsername());
                emailLabel.setText(currentUser.getEmail());
            }
        }
        
        // Cerchio centrato correttamente per un'immagine 100x100
        Circle clip = new Circle(50, 50, 50);
        avatarImageView.setClip(clip);
    }

    @FXML
    private void handleChangeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona Foto Profilo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg"));
        
        File file = fileChooser.showOpenDialog(avatarImageView.getScene().getWindow());

        if (file != null) {
            try {
                byte[] rawBytes = Files.readAllBytes(file.toPath());
                // Compressione tramite la tua classe ImageUtil
                this.selectedAvatarBytes = ImageUtil.compressImage(rawBytes, 250, 0.7f);
                
                if (selectedAvatarBytes != null) {
                    avatarImageView.setImage(new Image(new ByteArrayInputStream(selectedAvatarBytes)));
                }
            } catch (Exception e) {
                ErrorUtil.log("Errore nel caricamento dell'avatar", e);
            }
        }
    }

    @FXML
    private void handleSave() {
        String newUsername = usernameField.getText().trim();
        if (newUsername.isEmpty()) {
            showWarning("L'username non può essere vuoto.");
            return;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            UserDAO dao = new UserDAO(conn);
            boolean success = true;

            // 1. Update Username
            if (!newUsername.equals(currentUser.getUsername())) {
                if (dao.updateUsername(currentUser.getId(), newUsername)) {
                    currentUser.setUsername(newUsername);
                } else { success = false; }
            }

            // 2. Update Avatar
            if (selectedAvatarBytes != null && !Arrays.equals(selectedAvatarBytes, currentUser.getAvatar())) {
                if (dao.updateAvatar(currentUser.getId(), selectedAvatarBytes)) {
                    currentUser.setAvatar(selectedAvatarBytes);
                    // Non tocchiamo SessionUtil come richiesto.
                } else { success = false; }
            }

            if (success) {
                showInfo("Profilo aggiornato con successo!");
            } else {
                showError("Errore durante l'aggiornamento.");
            }

        } catch (Exception e) {
            ErrorUtil.log("Errore di connessione durante il salvataggio del profilo", e);
            showError("Errore di connessione.");
        }
    }

    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).show(); }
    private void showWarning(String msg) { new Alert(Alert.AlertType.WARNING, msg).show(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).show(); }
}