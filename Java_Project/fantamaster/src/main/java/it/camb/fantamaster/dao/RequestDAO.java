
package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.RequestStatus;

public class RequestDAO {
    private final Connection conn;

    public RequestDAO(Connection conn) {
        this.conn = conn;
    }

    //query
    //ottieni le richieste di iscrizione per una lega
    public List<Request> getRequestsForLeague(League league) {
    String sql = """
        SELECT id, utente_id, lega_id, data_richiesta, stato
        FROM richieste_accesso
        WHERE lega_id = ? AND stato = 'in_attesa'
        ORDER BY data_richiesta ASC
    """;

    List<Request> requests = new ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, league.getId());

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id"); // id della richiesta
                int userId = rs.getInt("utente_id"); // id dell'utente che ha fatto la richiesta
                int leagueId = rs.getInt("lega_id"); // id della lega per cui è stata fatta la richiesta
                RequestStatus status = RequestStatus.fromDb(rs.getString("stato")); // stato della richiesta
                java.sql.Timestamp ts = rs.getTimestamp("data_richiesta"); // timestamp della richiesta

                UserDAO userDAO = new UserDAO(conn);
                User user = userDAO.findById(userId);
                // Se ti serve, carica altri campi: userDAO.findById(userId)
                Request request = new Request(league, user, status);
                request.setTimestamp(ts != null ? ts.toLocalDateTime() : null);

                /*
                Request request = new Request(
                    id,
                    league,
                    user,
                    ts != null ? ts.toLocalDateTime() : null,
                    RequestStatus.fromDb(statoDb)
                );*/
                requests.add(request);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return java.util.Collections.emptyList();
    }
    return requests;
}
    //vecchia versione
    /* 
    public List<Request> getRequestsForLeague(League league) {
        String sql = "SELECT * FROM richieste_accesso WHERE lega_id = ? AND stato = 'in_attesa'";
        List<Request> requests = new java.util.ArrayList<>();

        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, league.getId());

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("utente_id");
                    User user = new User();
                    user.setId(userId);
                    // Potresti voler caricare più dettagli dell'utente qui

                    Request request = new Request(league, user, rs.getBoolean("accepted"));
                    requests.add(request);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
        return requests;
    }*/

    //crea una richiesta di iscrizione
    public boolean createRequest(User user, League league) {
        String sql = "INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (?, ?, ?)";

        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            stmt.setString(3, RequestStatus.in_attesa.name());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    //elimina una richiesta di iscrizione
    public boolean deleteRequest(User user, League league) {
        String sql = "DELETE FROM richieste_accesso WHERE utente_id = ? AND lega_id = ?";

        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //approva una richiesta di iscrizione
    public boolean approveRequest(User user, League league) {
        String sql = "UPDATE richieste_accesso SET stato = ? WHERE utente_id = ? AND lega_id = ?";
        //il server del database si occupa di aggiungere l'utente alla lega una volta che la richiesta è approvata e di eliminare la richiesta
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, RequestStatus.accettata.toDb());
            stmt.setInt(2, user.getId());
            stmt.setInt(3, league.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //rifuta una richiesta di iscrizione con eliminazione immediata
    public boolean rejectRequest(User user, League league) {
        return deleteRequest(user, league);
    }
}
