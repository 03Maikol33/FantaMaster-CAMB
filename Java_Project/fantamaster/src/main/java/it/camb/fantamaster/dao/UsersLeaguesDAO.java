package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;

public class UsersLeaguesDAO {

    private final Connection connection;

    public UsersLeaguesDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Recupera la lista degli utenti iscritti a una lega dato l'ID della lega.
     * Questo metodo è usato da LeagueDAO durante la costruzione dell'oggetto League.
     */
    public List<User> getUsersInLeagueId(int leagueId) {
        List<User> users = new ArrayList<>();
        
        // CORREZIONE: ul.lega_id (invece di id_leghe)
        String sql = "SELECT u.* FROM utenti u " +
                     "JOIN utenti_leghe ul ON u.id = ul.utente_id " +
                     "WHERE ul.lega_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setHashPassword(rs.getString("hash_password"));
                    
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        user.setCreatedAt(ts.toLocalDateTime());
                    }

                    // Nota: se l'utente ha un avatar, dovresti mapparlo qui
                    // Blob blob = rs.getBlob("avatar"); ...

                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Wrapper per comodità
    public List<User> getUsersInLeague(League league) {
        return getUsersInLeagueId(league.getId());
    }

    // Iscrive un utente a una lega
    public boolean subscribeUserToLeague(User user, League league) {
        // CORREZIONE: lega_id (invece di id_leghe)
        String sql = "INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            // Già iscritto
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verifica se l'utente è iscritto a una lega
    public boolean isUserSubscribed(User user, League league) {
        // CORREZIONE: lega_id (invece di id_leghe)
        String sql = "SELECT 1 FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Rimuove l'iscrizione
    public boolean unsubscribeUserFromLeague(User user, League league) {
        // CORREZIONE: lega_id (invece di id_leghe)
        String sql = "DELETE FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // ==========================================================
    // SEZIONE CLASSIFICA 
    // ==========================================================

    /**
     * Aggiorna il punteggio totale di TUTTI i partecipanti di una lega.
     * Somma tutti i 'fantavoto_calcolato' presenti in dettaglio_formazione per quella squadra.
     * Da chiamare alla fine del calcolo di una giornata.
     */
    public void updateLeagueRanking(int leagueId) {
        // 1. Prende la tabella utenti_leghe (le squadre)
        // 2. Fa una JOIN con formazioni e dettaglio_formazione
        // 3. Somma i voti calcolati
        // 4. Aggiorna il campo punteggio_totale della squadra
        
        String sql = "UPDATE utenti_leghe ul " +
                     "SET ul.punteggio_totale = ( " +
                     "    SELECT COALESCE(SUM(df.fantavoto_calcolato), 0) " +
                     "    FROM formazioni f " +
                     "    JOIN dettaglio_formazione df ON f.id = df.formazione_id " +
                     // Nota: Assicurati che la tua tabella formazioni usi 'rosa_id' o 'id_utente'+'id_lega'
                     // Qui uso una logica generica basata sulla rosa/squadra
                     "    WHERE f.rosa_id = (SELECT id FROM rosa WHERE utenti_leghe_id = ul.id) " +
                     ") " +
                     "WHERE ul.lega_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            int updatedRows = stmt.executeUpdate();
            System.out.println("✅ Classifica aggiornata per lega " + leagueId + ". Squadre aggiornate: " + updatedRows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recupera la classifica completa della lega, ordinata per punteggio totale decrescente.
     */
    public List<UserRankingRow> getLeagueRanking(int leagueId) {
        List<UserRankingRow> ranking = new ArrayList<>();
        
        String sql = "SELECT u.username, ul.nome_squadra, ul.punteggio_totale " +
                     "FROM utenti_leghe ul " +
                     "JOIN utenti u ON ul.utente_id = u.id " +
                     "WHERE ul.lega_id = ? " +
                     "ORDER BY ul.punteggio_totale DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                int position = 1;
                while (rs.next()) {
                    ranking.add(new UserRankingRow(
                        position++,
                        rs.getString("username"),
                        rs.getString("nome_squadra"),
                        rs.getDouble("punteggio_totale")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ranking;
    }
    
    // Classe di supporto interna (o esterna) per rappresentare una riga di classifica
    public static class UserRankingRow {
        public int posizione;
        public String username;
        public String nomeSquadra;
        public double punteggio;

        public UserRankingRow(int posizione, String username, String nomeSquadra, double punteggio) {
            this.posizione = posizione;
            this.username = username;
            this.nomeSquadra = nomeSquadra;
            this.punteggio = punteggio;
        }

        public int getPosizione() { return posizione; }
        public String getUsername() { return username; }
        public String getNomeSquadra() { return nomeSquadra; }
        public double getPunteggio() { return punteggio; }
    }


}

