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
     */
    public boolean saveFormation(int userId, int leagueId, String modulo, Player capitano, List<Player> titolari, List<Player> panchina) throws SQLException {
        // 1. Cerchiamo la giornata attiva ('da_giocare')
        String checkGiornata = "SELECT id FROM giornate WHERE stato = 'da_giocare' LIMIT 1";
        int giornataId = -1;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(checkGiornata)) {
            if (rs.next()) giornataId = rs.getInt("id");
        }
        
        if (giornataId == -1) throw new SQLException("Nessuna giornata 'da_giocare' trovata.");

        // 2. Inseriamo o aggiorniamo la testata della formazione
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
                stmt.setInt(5, capitano.getId());
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) formazioneId = rs.getInt(1);
                    else {
                        // Se non ha generato chiavi (Duplicate Key), lo recuperiamo
                        String find = "SELECT id FROM formazioni WHERE rosa_id = (SELECT id FROM rosa WHERE utenti_leghe_id = (SELECT id FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?)) AND giornata_id = ?";
                        PreparedStatement pst = conn.prepareStatement(find);
                        pst.setInt(1, userId); pst.setInt(2, leagueId); pst.setInt(3, giornataId);
                        ResultSet rs2 = pst.executeQuery(); if (rs2.next()) formazioneId = rs2.getInt(1);
                    }
                }
            }

            if (formazioneId != -1) {
                // 3. Puliamo il vecchio dettaglio e inseriamo il nuovo
                conn.prepareStatement("DELETE FROM dettaglio_formazione WHERE formazione_id = " + formazioneId).executeUpdate();
                String sqlDet = "INSERT INTO dettaglio_formazione (formazione_id, giocatore_id, stato, ordine_panchina) VALUES (?, (SELECT id FROM giocatori WHERE id_esterno = ?), ?, ?)";
                try (PreparedStatement stmtDet = conn.prepareStatement(sqlDet)) {
                    for (Player p : titolari) {
                        stmtDet.setInt(1, formazioneId); stmtDet.setInt(2, p.getId());
                        stmtDet.setString(3, "titolare"); stmtDet.setInt(4, 0); stmtDet.addBatch();
                    }
                    int ordine = 1;
                    for (Player p : panchina) {
                        stmtDet.setInt(1, formazioneId); stmtDet.setInt(2, p.getId());
                        stmtDet.setString(3, "panchina"); stmtDet.setInt(4, ordine++); stmtDet.addBatch();
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
    }
}