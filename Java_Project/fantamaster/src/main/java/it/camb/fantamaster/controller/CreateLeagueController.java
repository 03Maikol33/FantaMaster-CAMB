package it.camb.fantamaster.controller;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class CreateLeagueController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField maxPlayersField;

    @FXML
    private Label imageLabel;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button uploadImageBtn;

    @FXML
    private Button createLeagueBtn;

    private String imagePath;

    @FXML
    private void handleUploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona immagine");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg")
        );
        File selected = chooser.showOpenDialog(uploadImageBtn.getScene().getWindow());

        if (selected != null) {
            imagePath = selected.getAbsolutePath();
            imageLabel.setText(selected.getName());
        } else {
            imageLabel.setText("Nessuna immagine selezionata");
        }
    }

    @FXML
    private void handleCreateLeague() {
        String name = nameField.getText().trim();
        String maxStr = maxPlayersField.getText().trim();

        if (name.isEmpty() || maxStr.isEmpty()) {
            feedbackLabel.setText("Compila tutti i campi.");
            return;
        }

        try {
            int max = Integer.parseInt(maxStr);
            if (max <= 0) {
                feedbackLabel.setText("Inserisci un numero valido (> 0).");
                return;
            }
            feedbackLabel.setText("Lega \"" + name + "\" creata con " + max + " partecipanti!");

            //creo la lega nel sistema
            
            byte[] imageBytes = null;
            try {
                imageBytes = toByteArray(imagePath);
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
            feedbackLabel.setText("Inserisci un numero valido.");
        }
    }

    private static byte[] toByteArray(String imagePath) throws IOException {
        return Files.readAllBytes(Paths.get(imagePath));
    }
}
