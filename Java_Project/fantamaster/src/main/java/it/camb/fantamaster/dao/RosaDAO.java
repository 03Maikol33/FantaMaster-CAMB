package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
}