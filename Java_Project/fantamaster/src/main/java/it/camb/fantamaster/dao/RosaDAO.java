package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.util.ErrorUtil;

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
    /**
     * Recupera la lista completa dei giocatori (oggetti Player) appartenenti a una rosa.
     * Usato dal simulatore per schierare la formazione.
     */
    public List<Player> getPlayersByRosaId(int rosaId) {
        List<Player> players = new ArrayList<>();
        
        // Join tra la tabella di collegamento (giocatori_rose) e l'anagrafica (giocatori)
        String sql = "SELECT g.* FROM giocatori g " +
                     "JOIN giocatori_rose gr ON g.id = gr.giocatore_id " +
                     "WHERE gr.rosa_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rosaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Player p = new Player();
                    p.setId(rs.getInt("id"));
                    p.setNome(rs.getString("nome"));
                    p.setCognome("");
                    p.setRuolo(rs.getString("ruolo"));
                 //   p.setNumero(rs.getInt("numero"));
                    p.setSquadra(rs.getString("squadra_reale"));
                    p.setPrezzo(rs.getInt("quotazione_iniziale"));
                                       
                    players.add(p);
                }
            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore recupero giocatori per rosa ID: " + rosaId, e);
        }
        return players;
    }
    public boolean createDefaultRosa(int utentiLegheId) throws SQLException {
        // Usiamo INSERT IGNORE per evitare errori se per qualche motivo la rosa esiste già
        String sql = "INSERT IGNORE INTO rosa (utenti_leghe_id) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utentiLegheId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            ErrorUtil.log("Errore creazione rosa di default per utenti_leghe_id: " + utentiLegheId, e);
        }
        return false;
    }

    public boolean updateRosaInfo(int rosaId, String nuovoNome) throws SQLException {
    String sql = "UPDATE rosa SET nome_rosa = ? WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, nuovoNome);
      //  stmt.setString(2, nuovoLogo);
        stmt.setInt(2, rosaId);
        return stmt.executeUpdate() > 0;
    }
}

}