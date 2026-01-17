package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.PlayerDAO;
import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionProposePlayerController {

    @FXML private TableView<Player> playerTable;
    @FXML private TableColumn<Player, Integer> colPrezzo;
    @FXML private TableColumn<Player, String> colRuolo;
    @FXML private TableColumn<Player, String> colNome;
    @FXML private TableColumn<Player, String> colSquadra;
    @FXML private Label budgetLabel;
    @FXML private TextField offertaInizialeField;

    private League currentLeague;
    private AuctionMainContainerController parentContainer;

    private static final int MAX_P = 3;
    private static final int MAX_D = 8;
    private static final int MAX_C = 8;
    private static final int MAX_A = 6;

    /**
     * Inizializza la schermata per proporre un giocatore all'asta.
     *
     * @param league lega corrente
     */
    public void initData(League league) {
        this.currentLeague = league;
        setupTable();
        loadBudgetResiduo();
        loadFilteredPlayers();
    }

    /**
     * Imposta il container principale per permettere refresh e comunicazione.
     *
     * @param parentContainer controller del container principale
     */
    public void setParentContainer(AuctionMainContainerController parentContainer) {
        this.parentContainer = parentContainer;
    }

    /**
     * Configura le colonne della tabella dei giocatori.
     */
    private void setupTable() {
        colPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        colRuolo.setCellValueFactory(new PropertyValueFactory<>("ruolo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeCompleto"));
        colSquadra.setCellValueFactory(new PropertyValueFactory<>("squadra"));
    }

    /**
     * Carica il budget residuo dell'utente nella lega corrente.
     */
    private void loadBudgetResiduo() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            int myUserId = SessionUtil.getCurrentSession().getUser().getId();

            String sql = "SELECT r.crediti_residui FROM rosa r " +
                    "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                    "WHERE ul.utente_id = ? AND ul.lega_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, myUserId);
                stmt.setInt(2, currentLeague.getId());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int residuo = rs.getInt("crediti_residui");
                        budgetLabel.setText("Budget Residuo: " + residuo + " FM");
                    }
                }
            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore caricamento budget residuo", e);
        }
    }

    /**
     * Carica i giocatori disponibili filtrandoli in base agli slot liberi
     * e ai giocatori già acquistati nella lega.
     */
    private void loadFilteredPlayers() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            PlayerDAO playerDAO = new PlayerDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);

            int currentUserId = SessionUtil.getCurrentSession().getUser().getId();
            Rosa miaRosa = rosaDAO.getRosaByUserAndLeague(currentUserId, currentLeague.getId());

            if (miaRosa == null) {
                System.out.println("Rosa non trovata per il caricamento dei filtri.");
                return;
            }

            int pCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "P");
            int dCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "D");
            int cCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "C");
            int aCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "A");

            List<Integer> takenIds = rosaDAO.getIdsGiocatoriCompratiInLega(currentLeague.getId());
            List<Player> allPlayers = playerDAO.getAllPlayers();

            List<Player> available = allPlayers.stream()
                    .filter(p -> !takenIds.contains(p.getId()))
                    .filter(p -> {
                        String r = p.getRuolo().toUpperCase();
                        return switch (r) {
                            case "P" -> pCount < MAX_P;
                            case "D" -> dCount < MAX_D;
                            case "C" -> cCount < MAX_C;
                            case "A" -> aCount < MAX_A;
                            default -> false;
                        };
                    })
                    .collect(Collectors.toList());

            playerTable.setItems(FXCollections.observableArrayList(available));

        } catch (Exception e) {
            ErrorUtil.log("Errore caricamento giocatori filtrati", e);
        }
    }

    /**
     * Gestisce la chiamata del giocatore selezionato e l'avvio dell'asta.
     */
    @FXML
    private void handleChiamata() {
        Player scelto = playerTable.getSelectionModel().getSelectedItem();
        String offertaTesto = offertaInizialeField.getText();

        if (scelto == null) {
            showAlert("Attenzione", "Seleziona un giocatore.");
            return;
        }
        if (offertaTesto == null || offertaTesto.isEmpty()) {
            showAlert("Attenzione", "Inserisci l'offerta.");
            return;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            int offerta = Integer.parseInt(offertaTesto.trim());

            RosaDAO rosaDAO = new RosaDAO(conn);
            int currentUserId = SessionUtil.getCurrentSession().getUser().getId();
            Rosa miaRosa = rosaDAO.getRosaByUserAndLeague(currentUserId, currentLeague.getId());

            if (miaRosa == null) {
                showAlert("Errore", "Non hai una rosa in questa lega.");
                return;
            }

            if (offerta < scelto.getPrezzo()) {
                showAlert("Errore", "L'offerta deve essere >= " + scelto.getPrezzo());
                return;
            }
            if (offerta > miaRosa.getCreditiDisponibili()) {
                showAlert("Errore", "Crediti insufficienti!");
                return;
            }

            checkAndInsertPlayer(conn, scelto);

            LeagueDAO leagueDAO = new LeagueDAO(conn);
            boolean ok = leagueDAO.avviaAstaBustaChiusa(
                    currentLeague.getId(),
                    scelto.getId(),
                    miaRosa.getId(),
                    offerta
            );

            if (ok && parentContainer != null) {
                parentContainer.forceRefresh();
            }

        } catch (Exception e) {
            ErrorUtil.log("Errore avvio asta busta chiusa", e);
        }
    }

    /**
     * Inserisce il giocatore nel database se non esiste già.
     * Aggiorna i dati in caso di duplicato.
     *
     * @param conn connessione attiva
     * @param p    giocatore da inserire
     * @throws SQLException in caso di errore SQL
     */
    private void checkAndInsertPlayer(Connection conn, Player p) throws SQLException {
        String sql = "INSERT INTO giocatori (id, id_esterno, nome, squadra_reale, ruolo, quotazione_iniziale) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "nome = VALUES(nome), " +
                "squadra_reale = VALUES(squadra_reale), " +
                "ruolo = VALUES(ruolo), " +
                "quotazione_iniziale = VALUES(quotazione_iniziale)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getId());
            stmt.setInt(2, p.getId());
            stmt.setString(3, p.getNome() + " " + p.getCognome());
            stmt.setString(4, p.getSquadra());
            stmt.setString(5, p.getRuolo());
            stmt.setInt(6, p.getPrezzo());
            stmt.executeUpdate();
        }
    }

    /**
     * Mostra un popup informativo.
     *
     * @param title   titolo dell'alert
     * @param content contenuto del messaggio
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
