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
import it.camb.fantamaster.util.ErrorUtil;

public class LeagueDAO {
    private final Connection conn;

    public LeagueDAO(Connection conn) {
        this.conn = conn;
    }

    // Modifica mapResultSetToLeague in it.camb.fantamaster.dao.LeagueDAO

    private League mapResultSetToLeague(ResultSet rs) throws SQLException {
        // 1. LEGGI TUTTI I DATI "SEMPLICI" IMMEDIATAMENTE
        // Estraiamo tutto dal ResultSet prima di chiamare altri DAO
        int id = rs.getInt("id");
        String name = rs.getString("nome");
        int maxMembers = rs.getInt("max_membri");
        boolean closed = rs.getBoolean("iscrizioni_chiuse");
        boolean astaAperta = rs.getBoolean("asta_aperta");
        boolean mercatoAperto = rs.getBoolean("mercato_aperto");
        int idCreatore = rs.getInt("id_creatore");
        String inviteCode = rs.getString("codice_invito");
        String moduli = rs.getString("moduli_consentiti");
        String modalita = rs.getString("modalita");
        int budgetIniziale = rs.getInt("budget_iniziale");
        
        // Gestione null per i turni asta
        Integer turnoId = rs.getInt("turno_asta_utente_id");
        if (rs.wasNull()) turnoId = null;
        
        Integer giocatoreId = rs.getInt("giocatore_chiamato_id");
        if (rs.wasNull()) giocatoreId = null;

        Timestamp ts = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        Blob blob = rs.getBlob("icona");
        byte[] image = (blob != null) ? blob.getBytes(1, (int) blob.length()) : null;

        // 2. ORA CHE ABBIAMO SALVATO I DATI IN VARIABILI LOCALI, POSSIAMO USARE GLI ALTRI DAO
        // Questo evita il conflitto con il ResultSet principale (rs)
        UserDAO userDAO = new UserDAO(this.conn);
        User creator = userDAO.findById(idCreatore);

        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(this.conn);
        List<User> participants = ulDAO.getUsersInLeagueId(id); 

        // 3. COSTRUIAMO L'OGGETTO LEAGUE
        League league = new League(id, name, image, maxMembers, creator, createdAt, closed, participants, modalita, astaAperta);
        
        league.setInviteCode(inviteCode);
        league.setAllowedFormations(moduli);
        league.setInitialBudget(budgetIniziale > 0 ? budgetIniziale : 500);
        league.setMercatoAperto(mercatoAperto);
        league.setTurnoAstaUtenteId(turnoId);
        league.setGiocatoreChiamatoId(giocatoreId);

        return league;
    }
/* 
    // Metodo helper per mappare il ResultSet in un oggetto League
    private League mapResultSetToLeague(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("nome");
        int maxMembers = rs.getInt("max_membri");
        boolean closed = rs.getBoolean("iscrizioni_chiuse");
        boolean astaAperta = rs.getBoolean("asta_aperta");
        boolean mercatoAperto = rs.getBoolean("mercato_aperto");

        Timestamp ts = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        Blob blob = rs.getBlob("icona");
        byte[] image = (blob != null) ? blob.getBytes(1, (int) blob.length()) : null;
        
        String modalita = rs.getString("modalita"); 

        UserDAO userDAO = new UserDAO(this.conn);
        User creator = userDAO.findById(rs.getInt("id_creatore"));

        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(this.conn);
        List<User> participants = ulDAO.getUsersInLeagueId(id); 

        // Creiamo l'oggetto usando il costruttore completo
        League league = new League(id, name, image, maxMembers, creator, createdAt, closed, participants, modalita, astaAperta);
        
        // Settiamo il codice invito letto dal DB
        league.setInviteCode(rs.getString("codice_invito"));
        // Mappiamo i moduli consentiti
        league.setAllowedFormations(rs.getString("moduli_consentiti"));
        
        // Mappiamo il budget dalla tabella collegata 'regole'
        int budget = rs.getInt("budget_iniziale");
        league.setInitialBudget(budget > 0 ? budget : 500); // Fallback a 500 se 0 o null
        
        league.setMercatoAperto(mercatoAperto);
        
        int turnoId = rs.getInt("turno_asta_utente_id");
        if (!rs.wasNull()) {
            league.setTurnoAstaUtenteId(turnoId);
        }

        int giocatoreId = rs.getInt("giocatore_chiamato_id");
        if (!rs.wasNull()) {
            league.setGiocatoreChiamatoId(giocatoreId);
        }


        return league;
    }*/

    public boolean isMercatoAperto(int leagueId) {
        String sql = "SELECT mercato_aperto FROM leghe WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("mercato_aperto");
                }
            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore controllo stato mercato", e);
        }
        return false; // Default se non trovato o errore
    }

    public boolean updateMercato(boolean isOpen, int leagueId) {
        String sql = "UPDATE leghe SET mercato_aperto = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isOpen);
            stmt.setInt(2, leagueId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            ErrorUtil.log("Errore aggiornamento stato mercato", e);
            return false;
        }
    }

    // Metodo per aggiornare le regole (moduli)
    public boolean updateLeagueRules(int leagueId, String allowedFormations) {
        String sql = "UPDATE leghe SET moduli_consentiti = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, allowedFormations);
            stmt.setInt(2, leagueId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            ErrorUtil.log("Errore aggiornamento regole lega", e);
            return false;
        }
    }

    public List<League> getLeaguesForUser(User user) {
        List<League> leagues = new ArrayList<>();
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
            ErrorUtil.log("Errore recupero leghe per utente", e);
        }
        return leagues;
    }

    public List<League> getLeaguesCreatedByUser(User user) {
        List<League> leagues = new ArrayList<>();
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
            ErrorUtil.log("Errore recupero leghe create da utente", e);
        }
        return leagues;
    }

    public League getLeagueById(int id) {
        String sql = "SELECT l.*, r.budget_iniziale FROM leghe l " +
                    "LEFT JOIN regole r ON l.id = r.lega_id WHERE l.id = ?";
        
        League league = null;
        int idCreatore = -1;

        // STEP 1: Eseguiamo la query sulla lega e salviamo i dati in variabili locali
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    idCreatore = rs.getInt("id_creatore");
                    
                    // Creiamo l'oggetto con i dati "primitivi"
                    league = new League();
                    league.setId(rs.getInt("id"));
                    league.setName(rs.getString("nome"));
                    league.setMaxMembers(rs.getInt("max_membri"));
                    league.setRegistrationsClosed(rs.getBoolean("iscrizioni_chiuse"));
                    league.setAstaAperta(rs.getBoolean("asta_aperta"));
                    league.setMercatoAperto(rs.getBoolean("mercato_aperto"));
                    league.setInviteCode(rs.getString("codice_invito"));
                    league.setAllowedFormations(rs.getString("moduli_consentiti"));
                    league.setGameMode(rs.getString("modalita"));
                    
                    int budget = rs.getInt("budget_iniziale");
                    league.setInitialBudget(budget > 0 ? budget : 500);

                    Integer tId = rs.getInt("turno_asta_utente_id");
                    league.setTurnoAstaUtenteId(rs.wasNull() ? null : tId);
                    
                    Integer gId = rs.getInt("giocatore_chiamato_id");
                    league.setGiocatoreChiamatoId(rs.wasNull() ? null : gId);

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) league.setCreatedAt(ts.toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            ErrorUtil.log("Errore recupero lega per ID", e);
        }

        // STEP 2: Ora che il ResultSet della lega è CHIUSO, usiamo gli altri DAO
        // Usando la stessa 'conn' ma con nuovi statement
        if (league != null) {
            try {
                if (idCreatore != -1) {
                    league.setCreator(new UserDAO(this.conn).findById(idCreatore));
                }
                // Qui chiamiamo UsersLeaguesDAO in tutta sicurezza
                league.setParticipants(new UsersLeaguesDAO(this.conn).getUsersInLeagueId(id));
            } catch (Exception e) {
                ErrorUtil.log("Errore caricamento dettagli lega", e);
            }
        }

        return league;
    }
/* 
    public League getLeagueById(int id) {
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
            ErrorUtil.log("Errore recupero lega per ID", e);
        }
        return null;
    }*/

    public boolean insertLeague(League league) {
        String code = CodeGenerator.generateCode();
        league.setInviteCode(code);

        String sqlLeague = "INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, created_at, codice_invito, modalita) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
                
                String modalitaUI = league.getGameMode();
                String modalitaDB;
                
                if (modalitaUI != null && (modalitaUI.equalsIgnoreCase("Scontri Diretti") || modalitaUI.equals("scontri_diretti"))) {
                    modalitaDB = "scontri_diretti";
                } else {
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

            // 3. Inserimento Regole di Default
            RulesDAO regoleDAO = new RulesDAO(conn);
            regoleDAO.insertDefaultRules(generatedId);

            conn.commit(); // Conferma tutto
            return true;

        } catch (SQLException e) {
            ErrorUtil.log("Errore inserimento nuova lega", e);
            try { conn.rollback(); } catch (SQLException ex) { ErrorUtil.log("Errore rollback inserimento nuova lega", ex); }
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { ErrorUtil.log("Errore reset auto-commit inserimento nuova lega", e); }
        }
    }

    public boolean closeRegistrations(int leagueId) {
        String sql = "UPDATE leghe SET iscrizioni_chiuse = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            stmt.setInt(2, leagueId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            ErrorUtil.log("Errore chiusura iscrizioni lega", e);
            return false;
        }
    }

    public boolean deleteLeague(int leagueId) {
        String sql = "DELETE FROM leghe WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            return stmt.executeUpdate() == 1; 
        } catch (SQLException e) {
            ErrorUtil.log("Errore cancellazione lega", e);
            return false;
        }
    }  

    public boolean deleteLeague(League league) {
        return deleteLeague(league.getId());
    }

    public League findLeagueByInviteCode(String code) {
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
            ErrorUtil.log("Errore recupero lega per codice invito", e);
        }
        return null;
    }

    /**
     * Aggiorna lo stato del turno d'asta.
     * @param leagueId ID della lega
     * @param userId ID dell'utente a cui tocca chiamare (può essere NULL)
     * @param giocatoreId ID del giocatore chiamato (può essere NULL se fase di chiamata)
     */
    public boolean updateTurnoAsta(int leagueId, Integer userId, Integer giocatoreId) {
        String sql = "UPDATE leghe SET turno_asta_utente_id = ?, giocatore_chiamato_id = ? WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Gestione Nullable per l'Utente
            if (userId != null) {
                stmt.setInt(1, userId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            // Gestione Nullable per il Giocatore
            if (giocatoreId != null) {
                stmt.setInt(2, giocatoreId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setInt(3, leagueId);
            
            return stmt.executeUpdate() == 1;
            
        } catch (SQLException e) {
            ErrorUtil.log("Errore aggiornamento turno asta", e);
            return false;
        }
    }

    //avvio asta a busta chiusa
    public boolean avviaAstaBustaChiusa(int leagueId, int giocatoreId, int rosaId, int offerta) {
        String sqlUpdateLega = "UPDATE leghe SET giocatore_chiamato_id = ? WHERE id = ?";
        String sqlInsertOfferta = "INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, tipo, offerta) VALUES (?, ?, ?, 'offerta', ?)";

        try {
            conn.setAutoCommit(false); // Inizio transazione

            // 1. Aggiorniamo la lega con il giocatore chiamato
            try (PreparedStatement stmtLega = conn.prepareStatement(sqlUpdateLega)) {
                stmtLega.setInt(1, giocatoreId);
                stmtLega.setInt(2, leagueId);
                stmtLega.executeUpdate();
            }

            // 2. Inseriamo la tua offerta a busta chiusa nella tabella dedicata
            try (PreparedStatement stmtOfferta = conn.prepareStatement(sqlInsertOfferta)) {
                stmtOfferta.setInt(1, leagueId);
                stmtOfferta.setInt(2, giocatoreId);
                stmtOfferta.setInt(3, rosaId);
                stmtOfferta.setInt(4, offerta);
                stmtOfferta.executeUpdate();
            }

            conn.commit(); // Conferma tutto
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ErrorUtil.log("Errore rollback avvio asta busta chiusa", ex); }
            ErrorUtil.log("Errore avvio asta busta chiusa", e);
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { ErrorUtil.log("Errore reset auto-commit avvio asta busta chiusa", e); }
        }
    }
}