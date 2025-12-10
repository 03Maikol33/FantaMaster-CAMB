package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Helper per mappare il ResultSet in un oggetto Request.
     * Carica anche l'oggetto User completo.
     */
    private Request mapResultSetToRequest(ResultSet rs, League league) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("utente_id");
        String statusStr = rs.getString("stato");
        
        // Conversione sicura dell'enum
        RequestStatus status;
        try {
            // Assumendo che nel DB salvi le stringhe come "in_attesa", "accettata", ecc.
            // Se usi RequestStatus.fromDb() mantienilo, altrimenti usa valueOf case-insensitive
            status = RequestStatus.valueOf(statusStr); 
        } catch (IllegalArgumentException e) {
            status = RequestStatus.in_attesa; // Fallback
        }

        Timestamp ts = rs.getTimestamp("data_richiesta");
        java.time.LocalDateTime requestedAt = (ts != null) ? ts.toLocalDateTime() : null;

        // Carichiamo l'User completo usando UserDAO
        UserDAO userDAO = new UserDAO(conn);
        User user = userDAO.findById(userId);

        Request request = new Request(league, user, status);
        request.setId(id); // Assicurati che Request abbia setId
        request.setTimestamp(requestedAt);
        
        return request;
    }

    // Ottieni le richieste di iscrizione "in_attesa" per una lega
    public List<Request> getRequestsForLeague(League league) {
        String sql = "SELECT * FROM richieste_accesso WHERE lega_id = ? AND stato = 'in_attesa' ORDER BY data_richiesta ASC";
        List<Request> requests = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, league.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToRequest(rs, league));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return requests;
    }

    // Crea una richiesta di iscrizione
    public boolean createRequest(User user, League league) {
        // Controllo preventivo: se esiste già una richiesta pendente, non crearne un'altra
        if (hasPendingRequest(user, league)) {
            return false;
        }

        String sql = "INSERT INTO richieste_accesso (utente_id, lega_id, stato, data_richiesta) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            stmt.setString(3, RequestStatus.in_attesa.name()); // O .toDb() se lo hai implementato

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Metodo helper per evitare duplicati
    public boolean hasPendingRequest(User user, League league) {
        String sql = "SELECT 1 FROM richieste_accesso WHERE utente_id = ? AND lega_id = ? AND stato = 'in_attesa'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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

    // Elimina una richiesta (usato per rifiutare o annullare)
    public boolean deleteRequest(User user, League league) {
        String sql = "DELETE FROM richieste_accesso WHERE utente_id = ? AND lega_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Approva una richiesta.
     * 1. Iscrive l'utente alla lega (tabella utenti_leghe)
     * 2. Aggiorna lo stato della richiesta a 'accettata'
     * Esegue tutto in una TRANSAZIONE.
     */
    public boolean approveRequest(User user, League league) {
        String updateSql = "UPDATE richieste_accesso SET stato = ? WHERE utente_id = ? AND lega_id = ?";
        
        try {
            // 1. Inizio Transazione
            conn.setAutoCommit(false);

            // 2. Iscrizione Utente (usando UsersLeaguesDAO)
            UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(conn);
            boolean subscribed = ulDAO.subscribeUserToLeague(user, league);
            
            // Se l'iscrizione fallisce (e l'utente non era già iscritto), abortiamo
            // Nota: subscribeUserToLeague ritorna false se l'utente è già iscritto. 
            // Se è già iscritto, vogliamo comunque aggiornare la richiesta per pulizia.
            if (!subscribed && !ulDAO.isUserSubscribed(user, league)) {
                conn.rollback();
                return false;
            }

            // 3. Aggiornamento Stato Richiesta
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, RequestStatus.accettata.name()); // O .toDb()
                stmt.setInt(2, user.getId());
                stmt.setInt(3, league.getId());
                
                int rows = stmt.executeUpdate();
                if (rows == 0) {
                    // Se non troviamo la richiesta da aggiornare, qualcosa non va
                    conn.rollback();
                    return false;
                }
            }

            // 4. Commit
            conn.commit();
            
            // 5. Aggiorniamo il modello in memoria per riflettere il cambiamento
            // (Opzionale, ma buona pratica se l'oggetto user/league è usato nella UI subito dopo)
            league.addParticipant(user); 
            
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

    // Rifiuta una richiesta: la elimina dal DB
    public boolean rejectRequest(User user, League league) {
        return deleteRequest(user, league);
    }
}
// Fix conflitti definitivo