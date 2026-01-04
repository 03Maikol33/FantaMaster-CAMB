package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class LeagueAuctionDAOTest {

    private LeagueDAO leagueDAO;
    private UserDAO userDAO;
    private Connection connection;
    private User admin;
    private User participant;
    private League league;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        
        // Ricostruzione DB H2 con la struttura corretta (inclusa asta_aperta e turno_asta)
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, avatar BLOB)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, " +
                    "id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), " +
                    "modalita VARCHAR(50), budget_iniziale INT DEFAULT 500, moduli_consentiti VARCHAR(255), " +
                    "asta_aperta BOOLEAN DEFAULT TRUE, turno_asta_utente_id INT DEFAULT NULL, giocatore_chiamato_id INT DEFAULT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
        }

        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        // 1. Creazione Utenti
        admin = new User(); admin.setUsername("Admin"); admin.setEmail("a@t.com"); admin.setHashPassword("x");
        userDAO.insert(admin);
        
        participant = new User(); participant.setUsername("User1"); participant.setEmail("u@t.com"); participant.setHashPassword("x");
        userDAO.insert(participant);

        // 2. Creazione Lega (Asta APERTA)
        league = new League(0, "AstaLeague", null, 4, admin, LocalDateTime.now(), true, new ArrayList<>(), "punti_totali", true);
        leagueDAO.insertLeague(league);
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE utenti_leghe");
            stmt.execute("DROP TABLE regole");
            stmt.execute("DROP TABLE leghe");
            stmt.execute("DROP TABLE utenti");
        }
    }

    @Test
    public void testUpdateTurnoAsta() {
        // Verifica iniziale: nessun turno assegnato
        League l = leagueDAO.getLeagueById(league.getId());
        assertNull("All'inizio il turno deve essere null", l.getTurnoAstaUtenteId());

        // AZIONE: Assegno il turno al partecipante
        boolean updated = leagueDAO.updateTurnoAsta(league.getId(), participant.getId(), null);
        assertTrue("Update deve ritornare true", updated);

        // VERIFICA
        League updatedLeague = leagueDAO.getLeagueById(league.getId());
        assertNotNull("Il turno non deve essere null", updatedLeague.getTurnoAstaUtenteId());
        assertEquals("Il turno deve essere di User1", (Integer) participant.getId(), updatedLeague.getTurnoAstaUtenteId());
    }

    @Test
    public void testAstaStatusReading() {
        // Verifica che il DAO legga correttamente il flag asta_aperta
        League l = leagueDAO.getLeagueById(league.getId());
        assertTrue("L'asta dovrebbe risultare aperta", l.isAuctionOpen());
    }
}