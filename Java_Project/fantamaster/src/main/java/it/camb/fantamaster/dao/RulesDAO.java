package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RulesDAO {
    private final Connection conn;

    public RulesDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Inserisce le regole di default (es. Budget 500) per una nuova lega.
     * Questo metodo viene chiamato all'interno della transazione di creazione della lega.
     */
    public void insertDefaultRules(int leagueId) throws SQLException {
        String sql = "INSERT INTO regole (lega_id, budget_iniziale) VALUES (?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            stmt.setInt(2, 500); // Valore di default
            stmt.executeUpdate();
        }
    }

    /**
     * Aggiorna il budget iniziale per una lega specifica.
     */
    public boolean updateBudget(int leagueId, int newBudget) {
        String sql = "UPDATE regole SET budget_iniziale = ? WHERE lega_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newBudget);
            stmt.setInt(2, leagueId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}