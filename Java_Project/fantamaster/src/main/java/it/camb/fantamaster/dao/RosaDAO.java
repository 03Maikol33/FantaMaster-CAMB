package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.camb.fantamaster.model.Rosa;

public class RosaDAO {
    private final Connection conn;

    public RosaDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Recupera la Rosa di un utente specifico in una lega specifica.
     * Serve per capire chi stiamo visualizzando nella lista.
     */
    public Rosa getRosaByUserAndLeague(int userId, int leagueId) throws SQLException {
        // Query che unisce rosa e utenti_leghe per trovare la rosa giusta
        String sql = "SELECT r.id, r.nome_rosa " +
                     "FROM rosa r " +
                     "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                     "WHERE ul.utente_id = ? AND ul.lega_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Rosa(
                        rs.getInt("id"),
                        rs.getString("nome_rosa")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Conta quanti giocatori sono stati acquistati da questa rosa.
     * Serve per disabilitare il bottone se la rosa è piena (25/25).
     */
    public int countGiocatoriInRosa(int rosaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM giocatori_rose WHERE rosa_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Map<String, Integer> getCountGiocatoriPerRuolo(int rosaId) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        // Inizializziamo a zero per sicurezza
        counts.put("P", 0); counts.put("D", 0); counts.put("C", 0); counts.put("A", 0);

        String sql = "SELECT g.ruolo, COUNT(*) as totale " +
                    "FROM giocatori_rose gr " +
                    "JOIN giocatori g ON gr.giocatore_id = g.id " +
                    "WHERE gr.rosa_id = ? " +
                    "GROUP BY g.ruolo";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("ruolo"), rs.getInt("totale"));
                }
            }
        }
        return counts;
    }

    /**
     * Conta quanti giocatori di un determinato ruolo sono già presenti in una rosa.
     */
    public int countGiocatoriPerRuolo(int rosaId, String ruolo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM giocatori_rose gr " +
                    "JOIN giocatori g ON gr.giocatore_id = g.id " +
                    "WHERE gr.rosa_id = ? AND g.ruolo = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            stmt.setString(2, ruolo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }



    public List<Integer> getIdsGiocatoriCompratiInLega(int leagueId) throws SQLException {
        List<Integer> takenIds = new ArrayList<>();
        String sql = "SELECT gr.giocatore_id FROM giocatori_rose gr " +
                    "JOIN rosa r ON gr.rosa_id = r.id " +
                    "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                    "WHERE ul.lega_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    takenIds.add(rs.getInt("giocatore_id"));
                }
            }
        }
        return takenIds;
    }
}