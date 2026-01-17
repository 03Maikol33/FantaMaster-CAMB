package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.AuctionDAO;
import it.camb.fantamaster.dao.PlayerDAO;
import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.*;

public class AuctionBiddingRoomController {

    @FXML private Label nomeGiocatoreLabel;
    @FXML private Label ruoloLabel;
    @FXML private Label squadraLabel;
    @FXML private Label prezzoLabel;
    @FXML private Label statusLabel;
    @FXML private VBox biddingControls;
    @FXML private TextField offertaField;
    @FXML private Label budgetLabel;
    @FXML private Button btnInviaOfferta;

    private static final int MAX_P = 3;
    private static final int MAX_D = 8;
    private static final int MAX_C = 8;
    private static final int MAX_A = 6;
    private static final int MAX_ROSA_TOTAL = 25;

    private League currentLeague;
    private Player currentPlayer;
    private Rosa miaRosa;

    /**
     * Inizializza la schermata dell'asta caricando dati della lega,
     * del giocatore chiamato e della rosa dell'utente.
     *
     * @param league lega corrente in cui si svolge l'asta
     */
    public void initData(League league) {
        this.currentLeague = league;
        int currentUserId = SessionUtil.getCurrentSession().getUser().getId();

        boolean sonoIoIlChiamante =
                (league.getTurnoAstaUtenteId() != null &&
                 league.getTurnoAstaUtenteId() == currentUserId);

        biddingControls.setVisible(!sonoIoIlChiamante);
        biddingControls.setManaged(!sonoIoIlChiamante);
        statusLabel.setVisible(sonoIoIlChiamante);

        try {
            Connection conn = ConnectionFactory.getConnection();
            PlayerDAO playerDAO = new PlayerDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);
            AuctionDAO auctionDAO = new AuctionDAO(conn);

            if (league.getGiocatoreChiamatoId() != null) {
                this.currentPlayer = playerDAO.getPlayerById(league.getGiocatoreChiamatoId());
                if (currentPlayer != null) {
                    nomeGiocatoreLabel.setText(currentPlayer.getNome() + " " + currentPlayer.getCognome());
                    ruoloLabel.setText("Ruolo: " + currentPlayer.getRuolo());
                    squadraLabel.setText(currentPlayer.getSquadra());
                    prezzoLabel.setText(String.valueOf(currentPlayer.getPrezzo()));
                }
            }

            this.miaRosa = rosaDAO.getRosaByUserAndLeague(currentUserId, league.getId());

            if (miaRosa != null && currentPlayer != null) {
                budgetLabel.setText(miaRosa.getCreditiDisponibili() + " FM");

                if (!sonoIoIlChiamante &&
                        auctionDAO.hasUserAlreadyBid(league.getId(), currentPlayer.getId(), miaRosa.getId())) {

                    biddingControls.setDisable(true);
                    statusLabel.setText("Hai già inviato la tua offerta per questo giocatore.");
                    statusLabel.setVisible(true);
                    return;
                }

                int totalPlayers = rosaDAO.countGiocatoriInRosa(miaRosa.getId());
                String ruolo = currentPlayer.getRuolo().toUpperCase();
                int countPerRuolo = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), ruolo);

                boolean isRosaPiena = totalPlayers >= MAX_ROSA_TOTAL;
                boolean isRuoloPieno = false;

                if (ruolo.equals("P")) isRuoloPieno = countPerRuolo >= MAX_P;
                else if (ruolo.equals("D")) isRuoloPieno = countPerRuolo >= MAX_D;
                else if (ruolo.equals("C")) isRuoloPieno = countPerRuolo >= MAX_C;
                else if (ruolo.equals("A")) isRuoloPieno = countPerRuolo >= MAX_A;

                if ((isRosaPiena || isRuoloPieno) && !sonoIoIlChiamante) {
                    btnInviaOfferta.setDisable(true);
                    offertaField.setDisable(true);

                    String motivo = isRosaPiena ? "Rosa piena (25/25)!" : "Slot " + ruolo + " completati!";
                    offertaField.setPromptText(motivo);
                    statusLabel.setText("Non puoi offrire: " + motivo + " Clicca 'Passa'.");
                    statusLabel.setVisible(true);
                }
            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore caricamento dati asta", e);
        }
    }

    /**
     * Gestisce l'invio dell'offerta da parte dell'utente.
     * Valida l'importo e lo registra nel database.
     */
    @FXML
    private void handleInviaOfferta() {
        String testo = offertaField.getText();
        if (testo == null || testo.trim().isEmpty()) return;

        try {
            int valoreOfferta = Integer.parseInt(testo.trim());

            if (valoreOfferta < currentPlayer.getPrezzo()) {
                showAlert("Offerta troppo bassa", "Devi offrire almeno " + currentPlayer.getPrezzo());
                return;
            }
            if (miaRosa != null && valoreOfferta > miaRosa.getCreditiDisponibili()) {
                showAlert("Budget insufficiente", "Non hai abbastanza crediti!");
                return;
            }

            inviaOffertaAlDatabase(valoreOfferta, "offerta");

        } catch (NumberFormatException e) {
            showAlert("Errore", "Inserisci un numero intero.");
        }
    }

    /**
     * Registra il "passo" dell'utente nell'asta.
     */
    @FXML
    private void handlePassa() {
        inviaOffertaAlDatabase(0, "passo");
    }

    /**
     * Inserisce nel database l'offerta o il passo dell'utente.
     *
     * @param valore valore offerto (0 se passo)
     * @param tipo   tipo di azione ("offerta" o "passo")
     */
    private void inviaOffertaAlDatabase(int valore, String tipo) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, tipo, offerta) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentLeague.getId());
                stmt.setInt(2, currentPlayer.getId());
                stmt.setInt(3, miaRosa.getId());
                stmt.setString(4, tipo);

                if (tipo.equals("passo")) {
                    stmt.setNull(5, Types.INTEGER);
                } else {
                    stmt.setInt(5, valore);
                }
                stmt.executeUpdate();
            }

            int partecipantiTotali = currentLeague.getParticipants().size();
            int offertePervenute = countOfferteInDB(conn);

            if (offertePervenute >= partecipantiTotali) {
                scatenaCalcoloVincitore();
            } else {
                biddingControls.setDisable(true);
                statusLabel.setText("Azione registrata. In attesa degli altri partecipanti...");
                statusLabel.setVisible(true);
            }

        } catch (SQLException e) {
            ErrorUtil.log("Errore invio offerta asta", e);
        }
    }

    /**
     * Conta quante offerte sono già state registrate per il giocatore corrente.
     *
     * @param conn connessione attiva al database
     * @return numero di offerte registrate
     * @throws SQLException in caso di errore SQL
     */
    private int countOfferteInDB(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offerte_asta WHERE lega_id = ? AND giocatore_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentLeague.getId());
            stmt.setInt(2, currentPlayer.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Avvia il calcolo del vincitore dell'asta e mostra una notifica all'utente.
     */
    private void scatenaCalcoloVincitore() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            AuctionDAO auctionDAO = new AuctionDAO(conn);

            String esito = auctionDAO.chiudiAstaEAssegna(currentLeague.getId(), currentPlayer.getId());

            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("ASTA CONCLUSA");
                alert.setHeaderText("Il giocatore è stato assegnato!");
                alert.setContentText("Aggiudicato a: " + esito);
                alert.showAndWait();
            });

        } catch (SQLException e) {
            ErrorUtil.log("Errore invio offerta asta", e);
        }
    }

    /**
     * Mostra un alert informativo o di errore.
     *
     * @param title   titolo della finestra
     * @param content contenuto del messaggio
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
