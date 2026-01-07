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
    public List<User> getParticipants(int leagueId) {
        return getUsersInLeagueId(leagueId);
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


    public int subscribeUserToLeague(User user, League league) throws SQLException {
        String sql = "INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (?, ?)";

        // Chiediamo al driver di restituire le chiavi generate
        try (PreparedStatement stmt = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Restituiamo l'ID di utenti_leghe
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Se è già iscritto, dobbiamo recuperare l'ID esistente
            return getExistingSubscriptionId(user.getId(), league.getId());
        }
        return -1;
    }

    public int getExistingSubscriptionId(int userId, int leagueId) throws SQLException {
        String sql = "SELECT id FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }
/* 
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
    }*/

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
}
// Fix conflitti definitivo