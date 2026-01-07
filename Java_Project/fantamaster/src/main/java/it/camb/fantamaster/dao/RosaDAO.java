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
                    p.setCognome(rs.getString("cognome"));
                    p.setRuolo(rs.getString("ruolo"));
                    p.setNumero(rs.getInt("numero"));
                    p.setSquadra(rs.getString("squadra"));
                    p.setPrezzo(rs.getInt("prezzo"));
                    p.setNazionalita(rs.getString( "nazionalità"));
                    // Se hai altri campi nel DB (es. quotazione_iniziale), aggiungili qui
                    
                    players.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }
}