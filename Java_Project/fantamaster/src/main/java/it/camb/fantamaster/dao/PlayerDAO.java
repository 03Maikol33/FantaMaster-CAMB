package it.camb.fantamaster.dao;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.util.SessionUtil;

public class PlayerDAO {
    private static final String LISTONE_JSON_PATH = "/api/listone.json";
    private final Connection conn;
    private List<Player> cachedListone = null;

    // Costruttore che accetta la connessione per il DB
    public PlayerDAO(Connection conn) {
        this.conn = conn;
    }

    // ==========================================================
    // SEZIONE LISTONE (Sorgente JSON) - Per ListoneController
    // ==========================================================

    public List<Player> getAllPlayers() {
        if (cachedListone != null) return cachedListone;
        List<Player> players = new ArrayList<>();
        try (InputStream inputStream = PlayerDAO.class.getResourceAsStream(LISTONE_JSON_PATH)) {
            if (inputStream == null) return players;
            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
            for (JsonElement element : jsonArray) {
                players.add(parsePlayerFromJson(element.getAsJsonObject()));
            }
            cachedListone = players;
        } catch (Exception e) { e.printStackTrace(); }
        return players;
    }

    public List<Player> getPlayersPage(int offset, int limit) {
        List<Player> all = getAllPlayers();
        if (offset >= all.size()) return new ArrayList<>();
        return new ArrayList<>(all.subList(offset, Math.min(offset + limit, all.size())));
    }

    public int getTotalCount() { return getAllPlayers().size(); }

    public List<Player> getPlayersPageFiltered(int offset, int limit, String ruoloFilter, Integer minPrice, Integer maxPrice) {
        List<Player> filtered = getAllPlayers().stream()
            .filter(p -> (ruoloFilter == null || ruoloFilter.equals("Tutti") || p.getRuolo().equalsIgnoreCase(ruoloFilter)))
            .filter(p -> (minPrice == null || p.getPrezzo() >= minPrice))
            .filter(p -> (maxPrice == null || p.getPrezzo() <= maxPrice))
            .collect(Collectors.toList());
        if (offset >= filtered.size()) return new ArrayList<>();
        return new ArrayList<>(filtered.subList(offset, Math.min(offset + limit, filtered.size())));
    }

    public int getFilteredTotalCount(String ruoloFilter, Integer minPrice, Integer maxPrice) {
        return (int) getAllPlayers().stream()
            .filter(p -> (ruoloFilter == null || ruoloFilter.equals("Tutti") || p.getRuolo().equalsIgnoreCase(ruoloFilter)))
            .filter(p -> (minPrice == null || p.getPrezzo() >= minPrice))
            .filter(p -> (maxPrice == null || p.getPrezzo() <= maxPrice))
            .count();
    }

    private Player parsePlayerFromJson(JsonObject jsonObject) {
        return new Player(
            jsonObject.get("id").getAsInt(),
            jsonObject.get("nome").getAsString(),
            jsonObject.get("cognome").getAsString(),
            jsonObject.get("squadra").getAsString(),
            jsonObject.get("numero").getAsInt(),
            jsonObject.get("ruolo").getAsString(),
            jsonObject.get("prezzo").getAsInt(),
            jsonObject.get("nazionalita").getAsString()
        );
    }

    // ==========================================================
    // SEZIONE DATABASE (SQL v5.5) - Per FormationController
    // ==========================================================

    /**
     * Recupera i giocatori posseduti dall'utente nella lega specifica.
     */
    public List<Player> getTeamRosterByLeague(String leagueName) {
        List<Integer> playerIds = new ArrayList<>();
        if (SessionUtil.getCurrentSession() == null) return new ArrayList<>();

        String sql = "SELECT g.id_esterno FROM giocatori g " +
                     "JOIN giocatori_rose gr ON g.id = gr.giocatore_id " +
                     "JOIN rosa r ON gr.rosa_id = r.id " +
                     "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                     "JOIN leghe l ON ul.lega_id = l.id " +
                     "WHERE ul.utente_id = ? AND l.nome = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, SessionUtil.getCurrentSession().getUser().getId());
            stmt.setString(2, leagueName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) playerIds.add(rs.getInt("id_esterno"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return getAllPlayers().stream()
                .filter(p -> playerIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Salva la formazione: inserisce in formazioni e dettaglio_formazione.
     * AGGIORNAMENTO: Salva anche il flag 'capitano' nella tabella di dettaglio.
     */
    public boolean saveFormation(int userId, int leagueId, String modulo, Player capitano, List<Player> titolari, List<Player> panchina) throws SQLException {
        // 1. Cerchiamo la giornata attiva nel DB
        String sqlGiornata = "SELECT id FROM giornate WHERE stato = 'da_giocare' ORDER BY numero_giornata ASC LIMIT 1";
        int giornataId = -1;
        
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlGiornata)) {
            if (rs.next()) {
                giornataId = rs.getInt("id");
            }
        }

        // Se non c'è una giornata "da_giocare", non possiamo salvare!
        if (giornataId == -1) {
            throw new SQLException("Il campionato non è ancora iniziato o non ci sono giornate aperte.");
        }

        // 2. Recupero ID della Rosa
        String sqlRosa = "SELECT id FROM rosa WHERE utenti_leghe_id = (SELECT id FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?)";
        int rosaId = -1;
        try (PreparedStatement ps = conn.prepareStatement(sqlRosa)) {
            ps.setInt(1, userId);
            ps.setInt(2, leagueId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) rosaId = rs.getInt("id");
        }

        if (rosaId == -1) throw new SQLException("Rosa non trovata per l'utente.");

        // 3. Inserimento Formazione (Utilizziamo transazione)
        try {
            conn.setAutoCommit(false);
            
            String sqlForm = "INSERT INTO formazioni (rosa_id, giornata_id, modulo_schierato, capitano_id) " +
                            "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE modulo_schierato=VALUES(modulo_schierato), capitano_id=VALUES(capitano_id)";
            
            int formazioneId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sqlForm, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, rosaId);
                ps.setInt(2, giornataId);
                ps.setString(3, modulo);
                ps.setInt(4, capitano.getId());
                ps.executeUpdate();
                
                ResultSet rsKeys = ps.getGeneratedKeys();
                if (rsKeys.next()) formazioneId = rsKeys.getInt(1);
                else {
                    // Se è un update, recuperiamo l'id esistente
                    String find = "SELECT id FROM formazioni WHERE rosa_id=? AND giornata_id=?";
                    PreparedStatement psF = conn.prepareStatement(find);
                    psF.setInt(1, rosaId); psF.setInt(2, giornataId);
                    ResultSet rsF = psF.executeQuery();
                    if(rsF.next()) formazioneId = rsF.getInt("id");
                }
            }

            // 4. Pulizia e inserimento dettagli
            conn.prepareStatement("DELETE FROM dettaglio_formazione WHERE formazione_id = " + formazioneId).executeUpdate();
            
            String sqlDet = "INSERT INTO dettaglio_formazione (formazione_id, giocatore_id, stato, ordine_panchina) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psD = conn.prepareStatement(sqlDet)) {
                // Titolari
                for (Player p : titolari) {
                    psD.setInt(1, formazioneId); psD.setInt(2, p.getId());
                    psD.setString(3, "titolare"); psD.setInt(4, 0);
                    psD.addBatch();
                }
                // Panchina
                int o = 1;
                for (Player p : panchina) {
                    psD.setInt(1, formazioneId); psD.setInt(2, p.getId());
                    psD.setString(3, "panchina"); psD.setInt(4, o++);
                    psD.addBatch();
                }
                psD.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    /*public boolean saveFormation(int userId, int leagueId, String modulo, Player capitano, List<Player> titolari, List<Player> panchina) throws SQLException {
        // 1. Cerchiamo la giornata attiva ('da_giocare')
        String checkGiornata = "SELECT id FROM giornate WHERE stato = 'da_giocare' LIMIT 1";
        int giornataId = -1;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(checkGiornata)) {
            if (rs.next()) giornataId = rs.getInt("id");
        }
        
        // Se non trovi giornate attive, gestisci l'errore o usa un fallback
        if (giornataId == -1) {
             System.err.println("Nessuna giornata 'da_giocare' trovata nel DB.");
             // throw new SQLException("Impossibile salvare: Nessuna giornata attiva.");
             // Fallback temporaneo se stai testando senza giornate attive:
             giornataId = 1; 
        }

        // 2. Inseriamo o aggiorniamo la testata della formazione
        // Nota: Manteniamo capitano_id in 'formazioni' per ridondanza/sicurezza, ma lo usiamo soprattutto nei dettagli
        String sqlFormazione = "INSERT INTO formazioni (rosa_id, giornata_id, modulo_schierato, capitano_id) " +
                               "VALUES ((SELECT id FROM rosa WHERE utenti_leghe_id = (SELECT id FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?)), " +
                               "?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE modulo_schierato = VALUES(modulo_schierato), capitano_id = VALUES(capitano_id)";
        
        try {
            conn.setAutoCommit(false);
            int formazioneId = -1;
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlFormazione, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userId); stmt.setInt(2, leagueId);
                stmt.setInt(3, giornataId); stmt.setString(4, modulo);
                stmt.setInt(5, (capitano != null ? capitano.getId() : 0)); // Gestione null safe
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) formazioneId = rs.getInt(1);
                    else {
                        // Se è un UPDATE, recuperiamo l'ID esistente
                        String find = "SELECT id FROM formazioni WHERE rosa_id = (SELECT id FROM rosa WHERE utenti_leghe_id = (SELECT id FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?)) AND giornata_id = ?";
                        PreparedStatement pst = conn.prepareStatement(find);
                        pst.setInt(1, userId); pst.setInt(2, leagueId); pst.setInt(3, giornataId);
                        ResultSet rs2 = pst.executeQuery(); if (rs2.next()) formazioneId = rs2.getInt(1);
                    }
                }
            }

            if (formazioneId != -1) {
                // 3. Puliamo il vecchio dettaglio per questa formazione
                conn.prepareStatement("DELETE FROM dettaglio_formazione WHERE formazione_id = " + formazioneId).executeUpdate();

                // 4. Inseriamo il nuovo dettaglio CON LA COLONNA CAPITANO
                // Assicurati di aver aggiunto la colonna 'capitano' (BOOLEAN o TINYINT) al DB!
                String sqlDet = "INSERT INTO dettaglio_formazione (formazione_id, giocatore_id, stato, ordine_panchina, capitano) VALUES (?, (SELECT id FROM giocatori WHERE id_esterno = ?), ?, ?, ?)";
                
                try (PreparedStatement stmtDet = conn.prepareStatement(sqlDet)) {
                    
                    // --- TITOLARI ---
                    for (Player p : titolari) {
                        stmtDet.setInt(1, formazioneId); 
                        stmtDet.setInt(2, p.getId());
                        stmtDet.setString(3, "titolare"); 
                        stmtDet.setInt(4, 0);
                        
                        // Check Capitano
                        boolean isCap = (capitano != null && p.getId() == capitano.getId());
                        stmtDet.setBoolean(5, isCap);
                        
                        stmtDet.addBatch();
                    }

                    // --- PANCHINA ---
                    int ordine = 1;
                    for (Player p : panchina) {
                        stmtDet.setInt(1, formazioneId); 
                        stmtDet.setInt(2, p.getId());
                        stmtDet.setString(3, "panchina"); 
                        stmtDet.setInt(4, ordine++);
                        
                        // Check Capitano (anche se in panchina)
                        boolean isCap = (capitano != null && p.getId() == capitano.getId());
                        stmtDet.setBoolean(5, isCap);
                        
                        stmtDet.addBatch();
                    }
                    
                    stmtDet.executeBatch();
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }*/

    // ==========================================================
    // SEZIONE CALCOLO PUNTEGGI - (User Story: Punteggio Totale)
    // ==========================================================

    /**
     * Aggiorna il DB salvando il voto finale calcolato per ogni giocatore della formazione.
     * Da chiamare DOPO che TeamScoreCalculator ha fatto il suo lavoro.
     */
    public void updateCalculatedVotes(int userId, int leagueId, int giornata, List<it.camb.fantamaster.model.FormationPlayer> giocatori) throws SQLException {
        // 1. Recuperiamo l'ID della formazione
        String sqlId = "SELECT id FROM formazioni WHERE id_utente = ? AND id_lega = ? AND giornata_id = (SELECT id FROM giornate WHERE id = ?)"; // Assumendo che giornata sia l'ID o il numero
        // NOTA: Se 'giornata' è un numero (es. 1), adatta la query. Se usi l'ID giornata, va bene così.
        // Per sicurezza, se passi il numero giornata (1..38):
        // "SELECT id FROM formazioni WHERE id_utente = ? AND id_lega = ? AND giornata_id = ?";
        
        int formazioneId = -1;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM formazioni WHERE id_utente = ? AND id_lega = ? AND giornata_id = ?")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            stmt.setInt(3, giornata);
            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) formazioneId = rs.getInt("id");
            }
        }

        if (formazioneId == -1) return; // Nessuna formazione trovata

        // 2. Aggiorniamo i voti
        String sqlUpdate = "UPDATE dettaglio_formazione SET fantavoto_calcolato = ? WHERE formazione_id = ? AND giocatore_id = (SELECT id FROM giocatori WHERE id_esterno = ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
            for (it.camb.fantamaster.model.FormationPlayer fp : giocatori) {
                stmt.setDouble(1, fp.getFantavotoCalcolato());
                stmt.setInt(2, formazioneId);
                stmt.setInt(3, fp.getPlayer().getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    /**
     * METODO AGGIUNTO PER GLI SCAMBI: Recupera i giocatori di una rosa specifica.
     */
    public List<Player> getPlayersByRosa(int rosaId) {
        List<Integer> playerIds = new ArrayList<>();
        String sql = "SELECT g.id_esterno FROM giocatori g " +
                     "JOIN giocatori_rose gr ON g.id = gr.giocatore_id " +
                     "WHERE gr.rosa_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) playerIds.add(rs.getInt("id_esterno"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return getAllPlayers().stream()
                .filter(p -> playerIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Esegue la User Story: "Consulti tutte le righe di Dettaglio_formazione facendo sum..."
     * Restituisce il totale della squadra direttamente dal Database.
     */
    public double getTeamTotalScore(int userId, int leagueId, int giornata) {
        String sql = "SELECT SUM(df.fantavoto_calcolato) as totale " +
                     "FROM dettaglio_formazione df " +
                     "JOIN formazioni f ON df.formazione_id = f.id " +
                     "WHERE f.id_utente = ? AND f.id_lega = ? AND f.giornata_id = ?";
                     
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            stmt.setInt(3, giornata);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("totale");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // cercare un giocatore nel listone JSON partendo dal suo ID
    public Player getPlayerById(int id) {
        return getAllPlayers().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }
}