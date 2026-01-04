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
            // Tabella Utenti
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), " +
                    "hash_password VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, avatar BLOB)");

            // Tabella Leghe aggiornata con TUTTE le colonne richieste dal DAO 
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, " +
                    "id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), " +
                    "modalita VARCHAR(50), moduli_consentiti VARCHAR(255), " +
                    "asta_aperta BOOLEAN DEFAULT FALSE, " +
                    "turno_asta_utente_id INT DEFAULT NULL, " +
                    "giocatore_chiamato_id INT DEFAULT NULL, " +
                    "budget_iniziale INT DEFAULT 500)");

            // Tabella Regole con i nuovi parametri
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500, " +
                    "usa_modificatore_difesa BOOLEAN, bonus_gol DOUBLE, bonus_assist DOUBLE, bonus_imbattibilita DOUBLE, " +
                    "bonus_rigore_parato DOUBLE, bonus_fattore_campo DOUBLE, malus_gol_subito DOUBLE, malus_ammonizione DOUBLE, " +
                    "malus_espulsione DOUBLE, malus_rigore_sbagliato DOUBLE, malus_autogol DOUBLE)");

            // Tabella di relazione
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (" +
                    "utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
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