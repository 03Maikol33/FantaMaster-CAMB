package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.RequestStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RequestDAOTest {

    private RequestDAO requestDAO;
    private UserDAO userDAO;
    private LeagueDAO leagueDAO;
    private Connection connection;
    
    private User user;
    private League league;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        // Setup tabelle minime
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS richieste_accesso (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, stato VARCHAR(50), data_richiesta TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
        }

        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);
        requestDAO = new RequestDAO(connection);

        // Dati di base
        user = new User(); user.setUsername("Joiner"); user.setEmail("j@t.com"); user.setHashPassword("x");
        userDAO.insert(user);
        
        User creator = new User(); creator.setUsername("Creator"); creator.setEmail("c@t.com"); creator.setHashPassword("x");
        userDAO.insert(creator);

        league = new League(0, "Liga", null, 10, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali");
        leagueDAO.insertLeague(league);
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE richieste_accesso");
            stmt.execute("DROP TABLE utenti_leghe");
            stmt.execute("DROP TABLE regole"); // Creata da LeagueDAO
            stmt.execute("DROP TABLE leghe");
            stmt.execute("DROP TABLE utenti");
        }
    }

    @Test
    public void testCreateRequest() {
        boolean created = requestDAO.createRequest(user, league);
        assertTrue(created);
        
        List<Request> requests = requestDAO.getRequestsForLeague(league);
        assertEquals(1, requests.size());
        assertEquals(RequestStatus.in_attesa, requests.get(0).getRequestStatus());
    }

    @Test
    public void testApproveRequest() {
        requestDAO.createRequest(user, league);
        
        boolean approved = requestDAO.approveRequest(user, league);
        assertTrue(approved);
        
        // Verifica che non sia più "in attesa"
        List<Request> pending = requestDAO.getRequestsForLeague(league);
        assertTrue(pending.isEmpty());
        
        // Verifica iscrizione effettiva
        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(connection);
        assertTrue(ulDAO.isUserSubscribed(user, league));
    }
}