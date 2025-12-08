package it.camb.fantamaster.dao;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    private League mapResultSetToLeague(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("nome");
        int maxMembers = rs.getInt("max_membri");
        boolean closed = rs.getBoolean("iscrizioni_chiuse");
        
        Timestamp ts = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        Blob blob = rs.getBlob("icona");
        byte[] image = (blob != null) ? blob.getBytes(1, (int) blob.length()) : null;

        UserDAO userDAO = new UserDAO(this.conn);
        User creator = userDAO.findById(rs.getInt("id_creatore"));

        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(this.conn);
        List<User> participants = ulDAO.getUsersInLeagueId(id); 

        return new League(id, name, image, maxMembers, creator, createdAt, closed, participants);
    }

    public List<League> getLeaguesForUser(User user) {
        List<League> leagues = new ArrayList<>();
        // CORREZIONE QUI SOTTO: ul.lega_id e ul.utente_id (invece di id_leghe/id_utente)
        String sql = "SELECT l.* FROM leghe l JOIN utenti_leghe ul ON l.id = ul.lega_id WHERE ul.utente_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leagues.add(mapResultSetToLeague(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leagues;
    }

    public List<League> getLeaguesCreatedByUser(User user) {
        List<League> leagues = new ArrayList<>();
        // Qui id_creatore è corretto secondo il tuo SQL
        String sql = "SELECT * FROM leghe WHERE id_creatore = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leagues.add(mapResultSetToLeague(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leagues;
    }

    public League getLeagueById(int id) {
        String sql = "SELECT * FROM leghe WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLeague(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertLeague(League league) {
        String sqlLeague = "INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        // CORREZIONE QUI SOTTO: utente_id e lega_id
        String sqlRelation = "INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (?, ?)";

        try {
            conn.setAutoCommit(false);
            int generatedId = -1;

            try (PreparedStatement stmt = conn.prepareStatement(sqlLeague, Statement.RETURN_GENERATED_KEYS)) {
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

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creazione lega fallita.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedId = generatedKeys.getInt(1);
                        league.setId(generatedId);
                    } else {
                        throw new SQLException("Creazione lega fallita, ID mancante.");
                    }
                }
            }

            try (PreparedStatement stmtRel = conn.prepareStatement(sqlRelation)) {
                stmtRel.setInt(1, league.getCreator().getId());
                stmtRel.setInt(2, generatedId);
                stmtRel.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

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