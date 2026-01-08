package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SquadraController {
    @FXML private ImageView logoImageView;
    @FXML private Label nomeSquadraLabel;
    @FXML private TableView<Player> giocatoriTable;
    @FXML private TableColumn<Player, String> nomeCol;
    @FXML private TableColumn<Player, String> ruoloCol;
    @FXML private TableColumn<Player, Integer> valoreCol;

    private League league;
    private Rosa rosa;
    private RosaDAO rosaDAO;

    public void initData(League league) {
        this.league = league;
        try {
            this.rosaDAO = new RosaDAO(ConnectionFactory.getConnection());
            loadSquadraData();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadSquadraData() throws SQLException {
        int userId = SessionUtil.getCurrentSession().getUser().getId();
        this.rosa = rosaDAO.getRosaByUserAndLeague(userId, league.getId());

        if (rosa != null) {
            nomeSquadraLabel.setText(rosa.getNomeRosa() != null ? rosa.getNomeRosa() : "Senza Nome");
            
            // Imposta il logo se salvato
            if (rosa.getLogoPath() != null && !rosa.getLogoPath().isEmpty()) {
                File file = new File(rosa.getLogoPath());
                if (file.exists()) logoImageView.setImage(new Image(file.toURI().toString()));
            }

            // Configura le colonne con i campi di Player
            nomeCol.setCellValueFactory(new PropertyValueFactory<>("nomeCompleto"));
            ruoloCol.setCellValueFactory(new PropertyValueFactory<>("ruolo"));
            valoreCol.setCellValueFactory(new PropertyValueFactory<>("prezzo")); 

            // Carica i giocatori (metodo getGiocatoriDellaRosa aggiunto a RosaDAO)
            List<Player> giocatori = rosaDAO.getGiocatoriDellaRosa(rosa.getId());
            giocatoriTable.setItems(FXCollections.observableArrayList(giocatori));
        }
    }

    @FXML
    private void handleModificaSquadra() {
        // 1. Modifica Nome tramite Dialog
        TextInputDialog dialog = new TextInputDialog(rosa.getNomeRosa());
        dialog.setTitle("Modifica Squadra");
        dialog.setHeaderText("Personalizza la tua rosa");
        dialog.setContentText("Nuovo nome:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nuovoNome -> {
            // 2. Modifica Logo tramite FileChooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona Logo Squadra");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(nomeSquadraLabel.getScene().getWindow());

            String pathLogo = (selectedFile != null) ? selectedFile.getAbsolutePath() : rosa.getLogoPath();

            try {
                if (rosaDAO.updateRosaInfo(rosa.getId(), nuovoNome, pathLogo)) {
                    rosa.setNomeRosa(nuovoNome);
                    rosa.setLogoPath(pathLogo);
                    nomeSquadraLabel.setText(nuovoNome);
                    if (selectedFile != null) logoImageView.setImage(new Image(selectedFile.toURI().toString()));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }
}