package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.BidResult; // Creeremo questa piccola classe di supporto
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionDAO {
    private final Connection conn;

    public AuctionDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Logica core: Calcola il vincitore e aggiorna il database (Transazionale)
     */
    public String chiudiAstaEAssegna(int leagueId, int giocatoreId) throws SQLException {
        // 1. RACCOLTA OFFERTE (Punto 1)
        List<BidResult> bids = getOffertePerGiocatore(leagueId, giocatoreId);

        if (bids.isEmpty()) {
            resetLegaDopoAsta(leagueId);
            return "Nessuno (asta deserta)";
        }

        // 2. CALCOLO VINCITORE (Punto 2: Max + Random Tie-break)
        int maxOfferta = bids.stream().mapToInt(b -> b.getOfferta()).max().getAsInt();
        List<BidResult> potenzialiVincitori = new ArrayList<>();
        for (BidResult b : bids) {
            if (b.getOfferta() == maxOfferta) potenzialiVincitori.add(b);
        }

        // Sorteggio casuale tra i pareggi
        Collections.shuffle(potenzialiVincitori);
        BidResult vincitore = potenzialiVincitori.get(0);

        // 3. AGGIORNAMENTO DB (Punto 3 & 5: Transazione Atomica)
        try {
            conn.setAutoCommit(false);

            // A. Assegnazione giocatore alla rosa
            String sqlAssegna = "INSERT INTO giocatori_rose (rosa_id, giocatore_id, costo_acquisto) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlAssegna)) {
                ps.setInt(1, vincitore.getRosaId());
                ps.setInt(2, giocatoreId);
                ps.setInt(3, vincitore.getOfferta());
                ps.executeUpdate();
            }

            // B. Detrazione crediti dalla rosa
            String sqlBudget = "UPDATE rosa SET crediti_residui = crediti_residui - ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlBudget)) {
                ps.setInt(1, vincitore.getOfferta());
                ps.setInt(2, vincitore.getRosaId());
                ps.executeUpdate();
            }

            // C. Pulizia offerte per il prossimo giro
            String sqlClean = "DELETE FROM offerte_asta WHERE lega_id = ? AND giocatore_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlClean)) {
                ps.setInt(1, leagueId);
                ps.setInt(2, giocatoreId);
                ps.executeUpdate();
            }

            // D. RESET LEGA (Punto 5: turno_asta_utente_id a NULL)
            String sqlReset = "UPDATE leghe SET giocatore_chiamato_id = NULL, turno_asta_utente_id = NULL WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlReset)) {
                ps.setInt(1, leagueId);
                ps.executeUpdate();
            }

            conn.commit();
            return vincitore.getNomeRosa() + " (" + vincitore.getOfferta() + " FM)";

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private List<BidResult> getOffertePerGiocatore(int leagueId, int giocatoreId) throws SQLException {
        List<BidResult> results = new ArrayList<>();
        String sql = "SELECT o.rosa_id, o.offerta, r.nome_rosa FROM offerte_asta o " +
                     "JOIN rosa r ON o.rosa_id = r.id " +
                     "WHERE o.lega_id = ? AND o.giocatore_id = ? AND o.tipo = 'offerta'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leagueId);
            ps.setInt(2, giocatoreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new BidResult(rs.getInt("rosa_id"), rs.getInt("offerta"), rs.getString("nome_rosa")));
                }
            }
        }
        return results;
    }

    private void resetLegaDopoAsta(int leagueId) throws SQLException {
        String sql = "UPDATE leghe SET giocatore_chiamato_id = NULL, turno_asta_utente_id = NULL WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leagueId);
            ps.executeUpdate();
        }
    }

    //usato per notificare l'esito dell'asta nella UI
    public String getUltimoRisultatoAsta(int leagueId)  throws SQLException {
        // Cerchiamo l'ultimo giocatore inserito in giocatori_rose per questa lega
        String sql = "SELECT g.nome, r.nome_rosa, gr.costo_acquisto " +
                    "FROM giocatori_rose gr " +
                    "JOIN giocatori g ON gr.giocatore_id = g.id " +
                    "JOIN rosa r ON gr.rosa_id = r.id " +
                    "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                    "WHERE ul.lega_id = ? " +
                    "ORDER BY gr.id DESC LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leagueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nome") + " assegnato a " + 
                        rs.getString("nome_rosa") + " per " + 
                        rs.getInt("costo_acquisto") + " FM";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Nessun dato disponibile";
    }

    public boolean hasUserAlreadyBid(int leagueId, int giocatoreId, int rosaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offerte_asta WHERE lega_id = ? AND giocatore_id = ? AND rosa_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            stmt.setInt(2, giocatoreId);
            stmt.setInt(3, rosaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}