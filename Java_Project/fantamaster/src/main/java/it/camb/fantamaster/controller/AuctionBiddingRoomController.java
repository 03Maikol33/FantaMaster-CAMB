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

    public void initData(League league) {
        this.currentLeague = league;
        int currentUserId = SessionUtil.getCurrentSession().getUser().getId();

        // 1. Identifichiamo il chiamante
        boolean sonoIoIlChiamante = (league.getTurnoAstaUtenteId() != null && league.getTurnoAstaUtenteId() == currentUserId);

        // Gestione UI iniziale
        biddingControls.setVisible(!sonoIoIlChiamante);
        biddingControls.setManaged(!sonoIoIlChiamante);
        statusLabel.setVisible(sonoIoIlChiamante);

        try {
            Connection conn = ConnectionFactory.getConnection();
            PlayerDAO playerDAO = new PlayerDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);
            // Inizializziamo l'AuctionDAO per il controllo dell'offerta esistente
            AuctionDAO auctionDAO = new AuctionDAO(conn);

            // 2. Carichiamo il giocatore (necessario per ruolo e ID)
            if (league.getGiocatoreChiamatoId() != null) {
                this.currentPlayer = playerDAO.getPlayerById(league.getGiocatoreChiamatoId());
                if (currentPlayer != null) {
                    nomeGiocatoreLabel.setText(currentPlayer.getNome() + " " + currentPlayer.getCognome());
                    ruoloLabel.setText("Ruolo: " + currentPlayer.getRuolo());
                    squadraLabel.setText(currentPlayer.getSquadra());
                    prezzoLabel.setText(String.valueOf(currentPlayer.getPrezzo()));
                }
            }

            // 3. Recupero Rosa e check impedimenti
            this.miaRosa = rosaDAO.getRosaByUserAndLeague(currentUserId, league.getId());
            
            if (miaRosa != null && currentPlayer != null) {
                budgetLabel.setText(miaRosa.getCreditiDisponibili() + " FM");

                // --- CHECK 1: HAI GIÀ OFFERTO? (Risolve il bug del rientro nell'asta) ---
                // Se non sono il chiamante, verifico tramite DAO se ho già un record in offerte_asta
                if (!sonoIoIlChiamante && auctionDAO.hasUserAlreadyBid(league.getId(), currentPlayer.getId(), miaRosa.getId())) {
                    biddingControls.setDisable(true);
                    statusLabel.setText("Hai già inviato la tua offerta per questo giocatore.");
                    statusLabel.setVisible(true);
                    return; // Chiudiamo qui: se hai già offerto, non servono altri controlli
                }

                // --- CHECK 2: LOGICA LIMITI (PUNTO 2 & 3) ---
                int totalPlayers = rosaDAO.countGiocatoriInRosa(miaRosa.getId());
                String ruolo = currentPlayer.getRuolo().toUpperCase();
                int countPerRuolo = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), ruolo);

                boolean isRosaPiena = totalPlayers >= MAX_ROSA_TOTAL;
                boolean isRuoloPieno = false;

                if (ruolo.equals("P")) isRuoloPieno = countPerRuolo >= MAX_P;
                else if (ruolo.equals("D")) isRuoloPieno = countPerRuolo >= MAX_D;
                else if (ruolo.equals("C")) isRuoloPieno = countPerRuolo >= MAX_C;
                else if (ruolo.equals("A")) isRuoloPieno = countPerRuolo >= MAX_A;

                // Se la rosa è piena O il ruolo specifico è pieno (e non sono io il chiamante)
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
    
/* 
    public void initData(League league) {
        this.currentLeague = league;
        int currentUserId = SessionUtil.getCurrentSession().getUser().getId();

        // 1. Identifichiamo se l'utente è colui che ha proposto il giocatore
        // Il chiamante è memorizzato in turno_asta_utente_id
        boolean sonoIoIlChiamante = (league.getTurnoAstaUtenteId() != null && league.getTurnoAstaUtenteId() == currentUserId);

        // UI: Se sono il chiamante, non vedo i tasti per offrire (ho già offerto nella schermata precedente)
        biddingControls.setVisible(!sonoIoIlChiamante);
        biddingControls.setManaged(!sonoIoIlChiamante);
        statusLabel.setVisible(sonoIoIlChiamante);

        // 2. Caricamento dati giocatore dal JSON
        if (league.getGiocatoreChiamatoId() != null) {
            PlayerDAO playerDAO = new PlayerDAO();
            this.currentPlayer = playerDAO.getPlayerById(league.getGiocatoreChiamatoId());
            if (currentPlayer != null) {
                nomeGiocatoreLabel.setText(currentPlayer.getNome() + " " + currentPlayer.getCognome());
                ruoloLabel.setText("Ruolo: " + currentPlayer.getRuolo());
                squadraLabel.setText(currentPlayer.getSquadra());
                prezzoLabel.setText(String.valueOf(currentPlayer.getPrezzo()));
            }
        }

        // 3. Recupero info Rosa per budget e ID rosa (necessario per offerte_asta)
        try{
            Connection conn = ConnectionFactory.getConnection();
            this.miaRosa = new RosaDAO(conn).getRosaByUserAndLeague(currentUserId, league.getId());

            if(miaRosa != null) {
                budgetLabel.setText(miaRosa.getCreditiDisponibili() + " FM");
                

            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore caricamento dati asta", e);
        }
    }*/

    @FXML
    private void handleInviaOfferta() {
        String testo = offertaField.getText();
        if (testo == null || testo.trim().isEmpty()) return;

        try {
            int valoreOfferta = Integer.parseInt(testo.trim());

            // Validazione Punto 3 & 4
            if (valoreOfferta < currentPlayer.getPrezzo()) {
                showAlert("Offerta troppo bassa", "Devi offrire almeno " + currentPlayer.getPrezzo());
                return;
            }
            if (miaRosa != null && valoreOfferta > miaRosa.getCreditiDisponibili()) {
                showAlert("Budget insufficiente", "Non hai abbastanza crediti!");
                return;
            }

            // Invio offerta al DB
            inviaOffertaAlDatabase(valoreOfferta, "offerta");

        } catch (NumberFormatException e) {
            showAlert("Errore", "Inserisci un numero intero.");
        }
    }

    @FXML
    private void handlePassa() {
        // Se l'utente non è interessato, inviamo 'passo' con offerta NULL
        inviaOffertaAlDatabase(0, "passo");
    }

    private void inviaOffertaAlDatabase(int valore, String tipo) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            // 1. Inserimento del record nella tabella offerte_asta
            String sql = "INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, tipo, offerta) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentLeague.getId());
                stmt.setInt(2, currentPlayer.getId());
                stmt.setInt(3, miaRosa.getId());
                stmt.setString(4, tipo);
                
                if (tipo.equals("passo")) {
                    stmt.setNull(5, Types.INTEGER); // Offerta NULL per chi passa
                } else {
                    stmt.setInt(5, valore);
                }
                stmt.executeUpdate();
            }

            // 2. Controllo completamento: quante offerte abbiamo per questo giocatore?
            int partecipantiTotali = currentLeague.getParticipants().size();
            int offertePervenute = countOfferteInDB(conn);

            if (offertePervenute >= partecipantiTotali) {
                scatenaCalcoloVincitore();
            } else {
                // UI: Feedback di attesa
                biddingControls.setDisable(true);
                statusLabel.setText("Azione registrata. In attesa degli altri partecipanti...");
                statusLabel.setVisible(true);
            }

        } catch (SQLException e) {
            ErrorUtil.log("Errore invio offerta asta", e);
        }
    }

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

    private void scatenaCalcoloVincitore() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            AuctionDAO auctionDAO = new AuctionDAO(conn);
            
            // 1. Eseguiamo la chiusura
            String esito = auctionDAO.chiudiAstaEAssegna(currentLeague.getId(), currentPlayer.getId());

            // 2. Notifica immediata
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}