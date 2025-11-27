package it.camb.fantamaster.dao;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;

public class LeagueDAO {
    private final Connection conn;

    public LeagueDAO(Connection conn) {
        this.conn = conn;
    }

    public List<League> getLeaguesForUser(User user) {
        List<League> leagues = new ArrayList<>();

        String sql = "SELECT l.* FROM leghe l JOIN utenti_leghe ul ON l.id = ul.id_leghe WHERE ul.id_utente = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    League league = new League();
                    league.setId(rs.getInt("id"));
                    league.setName(rs.getString("nome"));
                    league.setMaxMembers(rs.getInt("max_membri"));

                    UserDAO userDAO = new UserDAO(this.conn);
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

                    leagues.add(league);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return leagues;
    }

    //ottieni leghe amministrate dall'utente
    public List<League> getLeaguesCreatedByUser(User user) {
        List<League> leagues = new ArrayList<>();

        String sql = "SELECT * FROM leghe WHERE id_creatore = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    League league = new League();
                    league.setId(rs.getInt("id"));
                    league.setName(rs.getString("nome"));
                    league.setMaxMembers(rs.getInt("max_membri"));

                    UserDAO userDAO = new UserDAO(this.conn);
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

                    leagues.add(league);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return leagues;
    }

    public boolean insertLeague(League league) {
        String sql = "INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, league.getName());

            if (league.getImage() != null) {
                stmt.setBlob(2, new ByteArrayInputStream(league.getImage()));
            } else {
                stmt.setNull(2, Types.BLOB);
            }

            stmt.setInt(3, league.getMaxMembers());
            stmt.setInt(4, league.getCreator().getId());
            stmt.setBoolean(5, league.isRegistrationsClosed());
            stmt.setTimestamp(6, Timestamp.valueOf(league.getCreatedAt()));

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public League getLeagueById(int id) {
        String sql = "SELECT * FROM leghe WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    League league = new League();
                    league.setId(rs.getInt("id"));
                    league.setName(rs.getString("nome"));
                    league.setMaxMembers(rs.getInt("max_membri"));
                    league.setRegistrationsClosed(rs.getBoolean("iscrizioni_chiuse"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        league.setCreatedAt(ts.toLocalDateTime());
                    }

                    Blob blob = rs.getBlob("icona");
                    if (blob != null) {
                        league.setImage(blob.getBytes(1, (int) blob.length()));
                    }

                    // impostare il creatore tramite UserDAO
                    UserDAO userDAO = new UserDAO(this.conn);
                    league.setCreator(userDAO.findById(rs.getInt("id_creatore")));

                    return league;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    //chiudi iscrizioni lega
    public boolean closeRegistrations(int leagueId) {
        String sql = "UPDATE leghe SET iscrizioni_chiuse = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            stmt.setInt(2, leagueId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
