package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rosa;

public class RosaDAO {
    private final Connection conn;

    public RosaDAO(Connection conn) {
        this.conn = conn;
    }


    public Rosa getRosaByUserAndLeague(int userId, int leagueId) throws SQLException {
        String sql = "SELECT r.* FROM rosa r " +
                    "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                    "WHERE ul.utente_id = ? AND ul.lega_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Usa i setter se il tuo costruttore non supporta tutti i campi
                    Rosa rosa = new Rosa(rs.getInt("id"), rs.getString("nome_rosa"));
                    rosa.setCreditiDisponibili(rs.getInt("crediti_residui")); 
                    return rosa;
                }
            }
        }
        return null;
    }

    /**
     * Recupera la Rosa di un utente specifico in una lega specifica.
     * Serve per capire chi stiamo visualizzando nella lista.
     *//* 
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
    }*/

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

    
    /**
     * Conta quanti giocatori di un determinato ruolo sono presenti in una rosa.
     */
    public int countGiocatoriPerRuolo(int rosaId, String ruolo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM giocatori_rose gr " +
                     "JOIN giocatori g ON gr.giocatore_id = g.id " +
                     "WHERE gr.rosa_id = ? AND g.ruolo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            stmt.setString(2, ruolo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Ritorna la lista degli ID dei giocatori già assegnati a qualche rosa nella stessa lega.
     */
    public List<Integer> getIdsGiocatoriCompratiInLega(int leagueId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT gr.giocatore_id FROM giocatori_rose gr " +
                     "JOIN rosa r ON gr.rosa_id = r.id " +
                     "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                     "WHERE ul.lega_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("giocatore_id"));
            }
        }
        return ids;
    }
/**
     * Conta i giocatori già acquistati dalla rosa, divisi per ruolo.
     * Restituisce una Mappa es: {"P": 2, "D": 5, "C": 8, "A": 1}
     */
    public Map<String, Integer> getRuoliCount(int rosaId) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("P", 0);
        counts.put("D", 0);
        counts.put("C", 0);
        counts.put("A", 0);

        String sql = "SELECT g.ruolo, COUNT(*) as totale " +
                     "FROM giocatori_rose gr " +
                     "JOIN giocatori g ON gr.giocatore_id = g.id " +
                     "WHERE gr.rosa_id = ? " +
                     "GROUP BY g.ruolo";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String ruolo = rs.getString("ruolo");
                    int totale = rs.getInt("totale");
                    // Assicuriamoci che il ruolo sia maiuscolo e pulito
                    if (ruolo != null) counts.put(ruolo.toUpperCase(), totale);
                }
            }
        }
        return counts;
    }

    public boolean createDefaultRosa(int utentiLegheId) throws SQLException {
        // Usiamo INSERT IGNORE per evitare errori se per qualche motivo la rosa esiste già
        String sql = "INSERT IGNORE INTO rosa (utenti_leghe_id) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utentiLegheId);
            return stmt.executeUpdate() == 1;
        }
    }

     // Aggiungi questi metodi in RosaDAO.java

   public List<Player> getGiocatoriDellaRosa(int rosaId) throws SQLException {
    List<Player> giocatori = new ArrayList<>();
    String sql = "SELECT g.* FROM giocatori g " +
                 "JOIN giocatori_rose gr ON g.id = gr.giocatore_id " +
                 "WHERE gr.rosa_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, rosaId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                giocatori.add(new Player(
                    rs.getInt("id"), rs.getString("nome"), rs.getString("cognome"),
                    rs.getString("squadra"), rs.getInt("numero"), rs.getString("ruolo"),
                    rs.getInt("prezzo"), rs.getString("nazionalita")
                ));
            }
        }
    }
    return giocatori;
    }

   public boolean updateRosaInfo(int rosaId, String nuovoNome, String nuovoLogo) throws SQLException {
    String sql = "UPDATE rosa SET nome_rosa = ?, logo_path = ? WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, nuovoNome);
        stmt.setString(2, nuovoLogo);
        stmt.setInt(3, rosaId);
        return stmt.executeUpdate() > 0;
    }
    }

}