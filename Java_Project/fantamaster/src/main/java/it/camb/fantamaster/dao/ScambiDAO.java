package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.camb.fantamaster.model.Scambio;

public class ScambiDAO {
    private final Connection conn;

    public ScambiDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean proponiScambio(Scambio s) throws SQLException {
        String sql = "INSERT INTO scambi (lega_id, rosa_richiedente_id, rosa_ricevente_id, giocatore_offerto_id, giocatore_richiesto_id, stato) VALUES (?, ?, ?, ?, ?, 'proposto')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, s.getLegaId());
            stmt.setInt(2, s.getRosaRichiedenteId());
            stmt.setInt(3, s.getRosaRiceventeId());
            stmt.setInt(4, s.getGiocatoreOffertoId());
            stmt.setInt(5, s.getGiocatoreRichiestoId());
            int res = stmt.executeUpdate();
            return res == 1;
        }
    }

    public List<Scambio> getScambiRicevuti(int rosaId) throws SQLException {
        List<Scambio> lista = new ArrayList<>();
        // FIX: Rimosso g1.cognome e g2.cognome perché non esistono nel DB
        String sql = "SELECT s.*, r1.nome_rosa as rich_nome, " +
                     "g1.nome as g_off_nome, g2.nome as g_riq_nome, " +
                     "g1.id_esterno as id_ext_off, g2.id_esterno as id_ext_riq " +
                     "FROM scambi s " +
                     "JOIN rosa r1 ON s.rosa_richiedente_id = r1.id " +
                     "JOIN giocatori g1 ON s.giocatore_offerto_id = g1.id " +
                     "JOIN giocatori g2 ON s.giocatore_richiesto_id = g2.id " +
                     "WHERE s.rosa_ricevente_id = ? AND s.stato = 'proposto'";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Scambio s = new Scambio();
                    s.setId(rs.getInt("id"));
                    s.setNomeRichiedente(rs.getString("rich_nome"));
                    s.setNomeGiocatoreOfferto(rs.getString("g_off_nome")); 
                    s.setNomeGiocatoreRichiesto(rs.getString("g_riq_nome"));
                    s.setGiocatoreOffertoId(rs.getInt("id_ext_off"));
                    s.setGiocatoreRichiestoId(rs.getInt("id_ext_riq"));
                    s.setRosaRichiedenteId(rs.getInt("rosa_richiedente_id"));
                    s.setRosaRiceventeId(rs.getInt("rosa_ricevente_id"));
                    lista.add(s);
                }
            }
        }
        return lista;
    }

    public int countRichiestePendenti(int rosaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM scambi WHERE rosa_ricevente_id = ? AND stato = 'proposto'";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                if (rs.next()) count = rs.getInt(1);
                return count;
            }
        }
    }

    public void rifiutaScambio(int scambioId) throws SQLException {
        String sql = "UPDATE scambi SET stato = 'rifiutato' WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, scambioId);
            stmt.executeUpdate();
        }
    }

    public void accettaScambio(Scambio s) throws SQLException {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement stS = conn.prepareStatement("UPDATE scambi SET stato = 'accettato' WHERE id = ?")) {
                stS.setInt(1, s.getId());
                stS.executeUpdate();
            }

            // Usiamo gli ID esterni per trovare i giocatori corretti nel DB
            String sqlUp = "UPDATE giocatori_rose SET rosa_id = ? WHERE rosa_id = ? AND giocatore_id = (SELECT id FROM giocatori WHERE id_esterno = ?)";
            
            try (PreparedStatement st1 = conn.prepareStatement(sqlUp)) {
                st1.setInt(1, s.getRosaRiceventeId());
                st1.setInt(2, s.getRosaRichiedenteId());
                st1.setInt(3, s.getGiocatoreOffertoId());
                st1.executeUpdate();
            }

            try (PreparedStatement st2 = conn.prepareStatement(sqlUp)) {
                st2.setInt(1, s.getRosaRichiedenteId());
                st2.setInt(2, s.getRosaRiceventeId());
                st2.setInt(3, s.getGiocatoreRichiestoId());
                st2.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}