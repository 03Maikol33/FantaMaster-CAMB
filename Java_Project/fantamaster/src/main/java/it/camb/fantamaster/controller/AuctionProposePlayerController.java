package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.PlayerDAO;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
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
    
    // Limiti hardcoded come richiesto
    private static final int MAX_P = 3;
    private static final int MAX_D = 8;
    private static final int MAX_C = 8;
    private static final int MAX_A = 6;

    public void initData(League league) {
        this.currentLeague = league;
        setupTable();
        loadBudgetResiduo();
        loadFilteredPlayers();
    }

    public void setParentContainer(AuctionMainContainerController parentContainer) {
        this.parentContainer = parentContainer;
    }

    private void setupTable() {
        colPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        colRuolo.setCellValueFactory(new PropertyValueFactory<>("ruolo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeCompleto"));
        colSquadra.setCellValueFactory(new PropertyValueFactory<>("squadra"));
    }

    private void loadBudgetResiduo() {
        try (Connection conn = ConnectionFactory.getConnection()) {
            int myUserId = SessionUtil.getCurrentSession().getUser().getId();
            
            // Query per prendere i crediti direttamente
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
            e.printStackTrace();
        }
    }

    private void loadFilteredPlayers() {
        try {
            Connection conn = ConnectionFactory.getConnection();
            // DAO JSON (Senza connessione)
            PlayerDAO playerDAO = new PlayerDAO(conn); 
            // DAO Database
            RosaDAO rosaDAO = new RosaDAO(conn);
            
            // 1. Dati dell'utente e della sua rosa
            int currentUserId = SessionUtil.getCurrentSession().getUser().getId();
            Rosa miaRosa = rosaDAO.getRosaByUserAndLeague(currentUserId, currentLeague.getId());
            
            // 2. Conteggio ruoli (User Story: slot liberi)
            // Usiamo il conteggio dei giocatori attualmente in rosa nel DB
            int pCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "P");
            int dCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "D");
            int cCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "C");
            int aCount = rosaDAO.countGiocatoriPerRuolo(miaRosa.getId(), "A");

            // 3. Lista ID "Blacklist" (Giocatori già comprati nella lega)
            List<Integer> takenIds = rosaDAO.getIdsGiocatoriCompratiInLega(currentLeague.getId());

            // 4. Carichiamo tutto dal JSON
            List<Player> allPlayersFromJson = playerDAO.getAllPlayers();

            // 5. APPLICAZIONE FILTRI (Streaming)
            List<Player> availableAndValid = allPlayersFromJson.stream()
                .filter(p -> !takenIds.contains(p.getId())) // Filtro 1: Non deve essere già preso
                .filter(p -> { // Filtro 2: Deve avere slot liberi (User Story)
                    String r = p.getRuolo().toUpperCase();
                    if (r.equals("P")) return pCount < MAX_P;
                    if (r.equals("D")) return dCount < MAX_D;
                    if (r.equals("C")) return cCount < MAX_C;
                    if (r.equals("A")) return aCount < MAX_A;
                    return false;
                })
                .collect(Collectors.toList());

            playerTable.setItems(FXCollections.observableArrayList(availableAndValid));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
    @FXML
    private void handleChiamata() {
        Player selected = playerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        // Qui implementerai la logica per salvare 'giocatore_chiamato_id' nel DB
        System.out.println("Hai chiamato: " + selected.getNome());
    }*/

    @FXML
    private void handleChiamata() {
        Player scelto = playerTable.getSelectionModel().getSelectedItem();
        String offertaTesto = offertaInizialeField.getText();

        if (scelto == null) { showAlert("Attenzione", "Seleziona un giocatore."); return; }
        if (offertaTesto == null || offertaTesto.isEmpty()) { showAlert("Attenzione", "Inserisci l'offerta."); return; }

        try{
            Connection conn = ConnectionFactory.getConnection();
            int offerta = Integer.parseInt(offertaTesto.trim());
            
            RosaDAO rosaDAO = new RosaDAO(conn);
            int currentUserId = SessionUtil.getCurrentSession().getUser().getId();
            // Recuperiamo l'oggetto Rosa completo (che contiene l'ID della rosa)
            Rosa miaRosa = rosaDAO.getRosaByUserAndLeague(currentUserId, currentLeague.getId());

            // --- VALIDAZIONI ---
            if (offerta < scelto.getPrezzo()) {
                showAlert("Errore", "L'offerta deve essere >= " + scelto.getPrezzo());
                return;
            }
            if (miaRosa != null && offerta > miaRosa.getCreditiDisponibili()) {
                showAlert("Errore", "Crediti insufficienti!");
                return;
            }

            // --- ESECUZIONE ---
            checkAndInsertPlayer(conn, scelto); // Lazy loading anagrafica

            LeagueDAO leagueDAO = new LeagueDAO(conn);
            // Passiamo l'ID della ROSA (miaRosa.getId()) come richiesto dal DB
            boolean ok = leagueDAO.avviaAstaBustaChiusa(currentLeague.getId(), scelto.getId(), miaRosa.getId(), offerta);

            if (ok) {
                System.out.println("Asta avviata correttamente.");
                if (parentContainer != null) parentContainer.forceRefresh();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* 
    @FXML
    private void handleChiamata() {
        Player scelto = playerTable.getSelectionModel().getSelectedItem();
        if (scelto == null) return;

        try (Connection conn = ConnectionFactory.getConnection()) {
            LeagueDAO leagueDAO = new LeagueDAO(conn);
            
            // 1. IMPORTANTE: Poiché usiamo Lazy Loading, dobbiamo assicurarci che 
            // il giocatore esista nella tabella 'giocatori' del DB prima di chiamarlo nell'asta.
            // Se non esiste, va inserito ora prendendo i dati dal modello Player (JSON).
            checkAndInsertPlayer(conn, scelto);

            // 2. Aggiorniamo la lega con l'ID del giocatore chiamato
            leagueDAO.updateTurnoAsta(currentLeague.getId(), SessionUtil.getCurrentSession().getUser().getId(), scelto.getId());
            
            System.out.println("Asta avviata per: " + scelto.getNome());
            // Il polling del container farà il resto!

            //forza il refresh della view nel parent container
            if (parentContainer != null) parentContainer.forceRefresh();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/
/*
    private void checkAndInsertPlayer(Connection conn, Player p) throws SQLException {
        String sql = "INSERT IGNORE INTO giocatori (id, id_esterno, nome, squadra_reale, ruolo, quotazione_iniziale) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getId());
            stmt.setInt(2, p.getId()); // Usiamo l'ID JSON come ID esterno
            stmt.setString(3, p.getNome() + " " + p.getCognome());
            stmt.setString(4, p.getSquadra());
            stmt.setString(5, p.getRuolo());
            stmt.setInt(6, p.getPrezzo());
            stmt.executeUpdate();
        }
    }*/

    private void checkAndInsertPlayer(Connection conn, Player p) throws SQLException {
        // Usiamo ON DUPLICATE KEY UPDATE per sovrascrivere i dati finti con quelli reali del JSON
        String sql = "INSERT INTO giocatori (id, id_esterno, nome, squadra_reale, ruolo, quotazione_iniziale) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "nome = VALUES(nome), " +
                    "squadra_reale = VALUES(squadra_reale), " +
                    "ruolo = VALUES(ruolo), " +
                    "quotazione_iniziale = VALUES(quotazione_iniziale)";
                    
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getId());
            stmt.setInt(2, p.getId()); // id_esterno
            stmt.setString(3, p.getNome() + " " + p.getCognome());
            stmt.setString(4, p.getSquadra());
            stmt.setString(5, p.getRuolo());
            stmt.setInt(6, p.getPrezzo());
            stmt.executeUpdate();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}