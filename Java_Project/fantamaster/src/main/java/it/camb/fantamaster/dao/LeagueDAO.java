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
import it.camb.fantamaster.util.CodeGenerator;
import it.camb.fantamaster.dao.RulesDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;

public class LeagueDAO {
    private final Connection conn;

    public LeagueDAO(Connection conn) {
        this.conn = conn;
    }

    // Metodo helper per mappare il ResultSet in un oggetto League
    // Esegue il mapping dei dati della lega e del budget dalla tabella regole collegata
    private League mapResultSetToLeague(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("nome");
        int maxMembers = rs.getInt("max_membri");
        boolean closed = rs.getBoolean("iscrizioni_chiuse");
        
        Timestamp ts = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        Blob blob = rs.getBlob("icona");
        byte[] image = (blob != null) ? blob.getBytes(1, (int) blob.length()) : null;
        
        // Leggiamo la stringa grezza dal DB (es. "punti_totali")
        String modalita = rs.getString("modalita"); 

        UserDAO userDAO = new UserDAO(this.conn);
        User creator = userDAO.findById(rs.getInt("id_creatore"));

        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(this.conn);
        List<User> participants = ulDAO.getUsersInLeagueId(id); 

        // Creiamo l'oggetto usando il costruttore completo aggiornato
        League league = new League(id, name, image, maxMembers, creator, createdAt, closed, participants, modalita);
        
        // Settiamo il codice invito letto dal DB
        league.setInviteCode(rs.getString("codice_invito"));
        
        // Mappiamo il budget dalla tabella collegata 'regole'
        // Se la colonna nel DB è NULL (o la join non trova righe), getInt restituisce 0
        int budget = rs.getInt("budget_iniziale");
        league.setInitialBudget(budget > 0 ? budget : 500); // Fallback a 500 se 0 o null
        
        return league;
    }

    public List<League> getLeaguesForUser(User user) {
        List<League> leagues = new ArrayList<>();
        // JOIN con tabella regole per ottenere il budget
        String sql = "SELECT l.*, r.budget_iniziale " +
                     "FROM leghe l " +
                     "JOIN utenti_leghe ul ON l.id = ul.lega_id " +
                     "LEFT JOIN regole r ON l.id = r.lega_id " +
                     "WHERE ul.utente_id = ?";
        
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
        // JOIN con tabella regole
        String sql = "SELECT l.*, r.budget_iniziale " +
                     "FROM leghe l " +
                     "LEFT JOIN regole r ON l.id = r.lega_id " +
                     "WHERE l.id_creatore = ?";
        
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
        // JOIN con tabella regole
        String sql = "SELECT l.*, r.budget_iniziale " +
                     "FROM leghe l " +
                     "LEFT JOIN regole r ON l.id = r.lega_id " +
                     "WHERE l.id = ?";
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
        String code = CodeGenerator.generateCode();
        league.setInviteCode(code);

        // Query per inserire la lega
        String sqlLeague = "INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, created_at, codice_invito) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Query per inserire il creatore come partecipante
        String sqlRelation = "INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (?, ?)";

        try {
            conn.setAutoCommit(false); // Inizio Transazione
            int generatedId = -1;

            // 1. Inserimento Lega
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
                stmt.setString(7, league.getInviteCode());
                
                // *** FIX PER "DATA TRUNCATED" ***
                // Convertiamo il valore dell'interfaccia (es. "Punti Totali") nel valore del DB (es. "punti_totali")
                String modalitaUI = league.getGameMode();
                String modalitaDB;
                
                if (modalitaUI != null && (modalitaUI.equalsIgnoreCase("Scontri Diretti") || modalitaUI.equals("scontri_diretti"))) {
                    modalitaDB = "scontri_diretti";
                } else {
                    // Default a "punti_totali" se è null, "Punti Totali", o qualsiasi altra cosa
                    modalitaDB = "punti_totali";
                }
                stmt.setString(8, modalitaDB);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creazione lega fallita.");

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedId = generatedKeys.getInt(1);
                        league.setId(generatedId);
                    } else {
                        throw new SQLException("Creazione lega fallita, ID mancante.");
                    }
                }
            }

            // 2. Inserimento Relazione Utente-Lega
            try (PreparedStatement stmtRel = conn.prepareStatement(sqlRelation)) {
                stmtRel.setInt(1, league.getCreator().getId());
                stmtRel.setInt(2, generatedId);
                stmtRel.executeUpdate();
            }

            // 3. Inserimento Regole di Default (USANDO IL NUOVO DAO)
            // Passiamo la stessa connessione transazionale!
            RulesDAO regoleDAO = new RulesDAO(conn);
            regoleDAO.insertDefaultRules(generatedId);

            conn.commit(); // Conferma tutto
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
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

    public boolean deleteLeague(int leagueId) {
        // La cancellazione a cascata del DB (ON DELETE CASCADE) eliminerà automaticamente
        // le righe corrispondenti in 'regole', 'utenti_leghe' e 'richieste_accesso'.
        String sql = "DELETE FROM leghe WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            return stmt.executeUpdate() == 1; 
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }  

    public boolean deleteLeague(League league) {
        return deleteLeague(league.getId());
    }

    public League findLeagueByInviteCode(String code) {
        // JOIN con tabella regole
        String sql = "SELECT l.*, r.budget_iniziale " +
                     "FROM leghe l " +
                     "LEFT JOIN regole r ON l.id = r.lega_id " +
                     "WHERE l.codice_invito = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
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
}