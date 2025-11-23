package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsersLeaguesDAO {

    private final Connection connection;

    public UsersLeaguesDAO(Connection connection) {
        this.connection = connection;
    }

    // ottieni gli utenti di una lega
    public List<User> getUsersInLeague(League league) {
        List<User> users = new ArrayList<>();

        String sql = "SELECT u.* FROM utenti u " +
                     "JOIN utenti_leghe ul ON u.id = ul.utente_id " +
                     "WHERE ul.lega_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, league.getId());
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

                    users.add(user);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    // Iscrive un utente a una lega
    public boolean subscribeUserToLeague(User user, League league) {
        String sql = "INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());

            stmt.executeUpdate();
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            // già iscritto
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verifica se l'utente è iscritto a una lega
    public boolean isUserSubscribed(User user, League league) {
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

    // Recupera tutte le leghe a cui è iscritto un utente
    public List<League> getLeaguesForUser(User user) {
        List<League> leagues = new ArrayList<>();

        String sql = "SELECT l.* FROM leghe l " +
                     "JOIN utenti_leghe ul ON l.id = ul.lega_id " +
                     "WHERE ul.utente_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, user.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    League league = new League();
                    league.setId(rs.getInt("id"));
                    league.setName(rs.getString("nome"));
                    league.setMaxMembers(rs.getInt("max_membri"));
                    UserDAO userDAO = new UserDAO(this.connection);
                    league.setCreator(userDAO.findById(rs.getInt("id_creatore")));
                    league.setRegistrationsClosed(rs.getBoolean("iscrizioni_chiuse"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        league.setCreatedAt(ts.toLocalDateTime());
                    }

                    Blob blob = rs.getBlob("icona");
                    if (blob != null) {
                        league.setImage(blob.getBytes(1, (int) blob.length()));
                    }

                    // Puoi caricare il creatore con UserDAO se serve
                    leagues.add(league);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return leagues;
    }

    // Rimuove l'iscrizione
    public boolean unsubscribeUserFromLeague(User user, League league) {
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
