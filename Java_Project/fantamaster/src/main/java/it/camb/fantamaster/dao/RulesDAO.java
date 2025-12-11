package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import it.camb.fantamaster.model.Rules;

public class RulesDAO {
    private final Connection conn;

    public RulesDAO(Connection conn) {
        this.conn = conn;
    }

    public void insertDefaultRules(int leagueId) throws SQLException {
        // Usa i default del DB
        String sql = "INSERT INTO regole (lega_id) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            stmt.executeUpdate();
        }
    }

    public Rules getRulesByLeagueId(int leagueId) {
        String sql = "SELECT * FROM regole WHERE lega_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Rules r = new Rules();
                    r.setId(rs.getInt("id"));
                    r.setLeagueId(rs.getInt("lega_id"));
                    r.setInitialBudget(rs.getInt("budget_iniziale"));
                    r.setUsaModificatoreDifesa(rs.getBoolean("usa_modificatore_difesa")); // NUOVO
                    
                    // Bonus
                    r.setBonusGol(rs.getDouble("bonus_gol"));
                    r.setBonusAssist(rs.getDouble("bonus_assist"));
                    r.setBonusImbattibilita(rs.getDouble("bonus_imbattibilita"));
                    r.setBonusRigoreParato(rs.getDouble("bonus_rigore_parato"));
                    r.setBonusFattoreCampo(rs.getDouble("bonus_fattore_campo"));
                    
                    // Malus
                    r.setMalusGolSubito(rs.getDouble("malus_gol_subito"));
                    r.setMalusAmmonizione(rs.getDouble("malus_ammonizione"));
                    r.setMalusEspulsione(rs.getDouble("malus_espulsione"));
                    r.setMalusRigoreSbagliato(rs.getDouble("malus_rigore_sbagliato"));
                    r.setMalusAutogol(rs.getDouble("malus_autogol"));
                    
                    return r;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Rules();
    }

    public boolean updateRules(int leagueId, Rules rules) {
        String sql = "UPDATE regole SET " +
                     "budget_iniziale = ?, usa_modificatore_difesa = ?, " +
                     "bonus_gol = ?, bonus_assist = ?, bonus_imbattibilita = ?, " +
                     "bonus_rigore_parato = ?, bonus_fattore_campo = ?, " +
                     "malus_gol_subito = ?, malus_ammonizione = ?, malus_espulsione = ?, " +
                     "malus_rigore_sbagliato = ?, malus_autogol = ? " +
                     "WHERE lega_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rules.getInitialBudget());
            stmt.setBoolean(2, rules.isUsaModificatoreDifesa()); // NUOVO
            
            stmt.setDouble(3, rules.getBonusGol());
            stmt.setDouble(4, rules.getBonusAssist());
            stmt.setDouble(5, rules.getBonusImbattibilita());
            stmt.setDouble(6, rules.getBonusRigoreParato());
            stmt.setDouble(7, rules.getBonusFattoreCampo());
            
            stmt.setDouble(8, rules.getMalusGolSubito());
            stmt.setDouble(9, rules.getMalusAmmonizione());
            stmt.setDouble(10, rules.getMalusEspulsione());
            stmt.setDouble(11, rules.getMalusRigoreSbagliato());
            stmt.setDouble(12, rules.getMalusAutogol());
            
            stmt.setInt(13, leagueId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}