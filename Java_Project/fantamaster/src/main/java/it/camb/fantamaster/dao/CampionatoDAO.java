package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CampionatoDAO {
    private Connection conn;

    public CampionatoDAO(Connection conn) {
        this.conn = conn;
    }

    public int getGiornataCorrente() {
        String sql = "SELECT giornata_corrente FROM stato_campionato WHERE id = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("giornata_corrente");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Metodo utile per l'Admin per far avanzare il campionato
    public void avanzaGiornata() {
        String sql = "UPDATE stato_campionato SET giornata_corrente = giornata_corrente + 1 WHERE id = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}