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

    /**
     * Mappa un ResultSet a un oggetto League.
     * Estrae i dati semplici dal ResultSet, recupera l'utente creatore dal database,
     * e carica la lista dei partecipanti.
     *
     * @param rs il ResultSet contenente i dati della lega
     * @return l'oggetto League costruito dai dati
     * @throws SQLException in caso di errore SQL
     */
    private League mapResultSetToLeague(ResultSet rs) throws SQLException {
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
        
        Integer turnoId = rs.getInt("turno_asta_utente_id");
        if (rs.wasNull()) turnoId = null;
        
        Integer giocatoreId = rs.getInt("giocatore_chiamato_id");
        if (rs.wasNull()) giocatoreId = null;

        Timestamp ts = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        Blob blob = rs.getBlob("icona");
        byte[] image = (blob != null) ? blob.getBytes(1, (int) blob.length()) : null;

        UserDAO userDAO = new UserDAO(this.conn);
        User creator = userDAO.findById(idCreatore);

        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(this.conn);
        List<User> participants = ulDAO.getUsersInLeagueId(id); 

        League league = new League(id, name, image, maxMembers, creator, createdAt, closed, participants, modalita, astaAperta);
        
        league.setInviteCode(inviteCode);
        league.setAllowedFormations(moduli);
        league.setInitialBudget(budgetIniziale > 0 ? budgetIniziale : 500);
        league.setMercatoAperto(mercatoAperto);
        league.setTurnoAstaUtenteId(turnoId);
        league.setGiocatoreChiamatoId(giocatoreId);

        return league;
    }

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

    /**
     * Recupera tutte le leghe a cui l'utente partecipa.
     *
     * @param user l'utente di cui recuperare le leghe
     * @return la lista delle leghe dell'utente
     */
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

    /**
     * Recupera tutte le leghe create da un utente.
     *
     * @param user l'utente creatore
     * @return la lista delle leghe create dall'utente
     */
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

    /**
     * Recupera una lega specifica dal suo ID.
     * Carica tutti i dati della lega inclusi il creatore e i partecipanti.
     *
     * @param id l'ID della lega
     * @return l'oggetto League, o null se non trovata
     */
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
                    Blob blob = rs.getBlob("icona");
                    if (blob != null) {
                        league.setImage(blob.getBytes(1, (int) blob.length()));
                    }
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

    public boolean updateLeagueIcon(int leagueId, byte[] imageBytes) {
        String sql = "UPDATE leghe SET icona = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (imageBytes != null) {
                stmt.setBlob(1, new java.io.ByteArrayInputStream(imageBytes));
            } else {
                stmt.setNull(1, java.sql.Types.BLOB);
            }
            stmt.setInt(2, leagueId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            ErrorUtil.log("Errore aggiornamento icona lega", e);
            return false;
        }
    }
}