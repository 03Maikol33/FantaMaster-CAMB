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

public class UsersLeaguesDAOTest {
    private UsersLeaguesDAO ulDAO;
    private UserDAO userDAO;
    private LeagueDAO leagueDAO;
    private Connection connection;
    private User user;
    private League league;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            // CORRETTO: Aggiunto 'asta_aperta BOOLEAN'
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), asta_aperta BOOLEAN DEFAULT FALSE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
        }
        ulDAO = new UsersLeaguesDAO(connection);
        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        user = new User(); user.setUsername("U"); user.setEmail("u@t.com"); user.setHashPassword("x");
        userDAO.insert(user);
        
        User creator = new User(); creator.setUsername("C"); creator.setEmail("c@t.com"); creator.setHashPassword("x");
        userDAO.insert(creator);
        
        // CORRETTO: Aggiunto 'false' finale
        league = new League(0, "L", null, 10, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", false);
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
    public void testSubscriptionFlow() {
        assertFalse(ulDAO.isUserSubscribed(user, league));
        
        boolean subscribed = ulDAO.subscribeUserToLeague(user, league);
        assertTrue(subscribed);
        
        assertTrue(ulDAO.isUserSubscribed(user, league));
        
        boolean unsubscribed = ulDAO.unsubscribeUserFromLeague(user, league);
        assertTrue(unsubscribed);
        assertFalse(ulDAO.isUserSubscribed(user, league));
    }
}