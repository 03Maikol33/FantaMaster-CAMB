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

public class LeagueDAOTest {

    private LeagueDAO leagueDAO;
    private UserDAO userDAO;
    private Connection connection;
    private User creator;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            
            // Tabella leghe allineata alla v5.5
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "nome VARCHAR(255), " +
                "icona BLOB, " +
                "max_membri INT, " +
                "id_creatore INT, " +
                "iscrizioni_chiuse BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP, " +
                "codice_invito VARCHAR(255), " +
                "modalita VARCHAR(50), " +
                "moduli_consentiti VARCHAR(255), " +
                "mercato_aperto BOOLEAN DEFAULT FALSE, " + // FONDAMENTALE
                "asta_aperta BOOLEAN DEFAULT TRUE, " +     // FONDAMENTALE
                "turno_asta_utente_id INT DEFAULT NULL, " +
                "giocatore_chiamato_id INT DEFAULT NULL)");

            // FONDAMENTALE: utenti_leghe deve avere la colonna 'id' PK
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            
            // Aggiungi la tabella ROSA perché molti DAO ora la usano nei Join
            stmt.execute("CREATE TABLE IF NOT EXISTS rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), punteggio_totale DOUBLE DEFAULT 0.0)");
        }

        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        // Setup utente per i test
        creator = new User();
        creator.setUsername("Creator");
        creator.setEmail("c@test.com");
        creator.setHashPassword("pass");
        userDAO.insert(creator);
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Pulisce tutto il DB in memoria per evitare inquinamento tra i test [cite: 41]
            stmt.execute("DROP ALL OBJECTS");
        }
    }

    @Test
    public void testInsertLeague() {
        League league = new League();
        league.setName("Serie A");
        league.setMaxMembers(8);
        league.setCreator(creator);
        league.setCreatedAt(LocalDateTime.now());
        league.setRegistrationsClosed(false);
        league.setParticipants(new ArrayList<>());
        league.setGameMode("punti_totali");

        boolean inserted = leagueDAO.insertLeague(league);
        assertTrue(inserted);
        assertNotNull(league.getInviteCode()); 
        assertNotEquals(0, league.getId());

        League retrieved = leagueDAO.getLeagueById(league.getId());
        assertNotNull(retrieved);
        assertEquals("Serie A", retrieved.getName());
    }

    @Test
    public void testGetLeaguesForUser() {
        // Usa il costruttore completo della classe League 
        League league = new League(0, "MyLeague", null, 10, creator, LocalDateTime.now(), false, new ArrayList<>(), "scontri_diretti", false);
        leagueDAO.insertLeague(league);

        var leagues = leagueDAO.getLeaguesForUser(creator);
        assertEquals(1, leagues.size());
        assertEquals("MyLeague", leagues.get(0).getName());
    }

    @Test
    public void testDeleteLeague() {
        League league = new League(0, "ToDelete", null, 10, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", false);
        leagueDAO.insertLeague(league);

        boolean deleted = leagueDAO.deleteLeague(league.getId());
        assertTrue(deleted);
        assertNull(leagueDAO.getLeagueById(league.getId()));
    }
}