package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Message;
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
import java.util.List;

import static org.junit.Assert.*;

public class MessageDAOTest {
    private MessageDAO messageDAO;
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
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS messaggi (id INT AUTO_INCREMENT PRIMARY KEY, testo TEXT, data_invio TIMESTAMP, utente_id INT, lega_id INT)");
        }
        messageDAO = new MessageDAO(connection);
        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        user = new User(); user.setUsername("Chatter"); user.setEmail("chat@t.com"); user.setHashPassword("x");
        userDAO.insert(user);
        league = new League(0, "ChatLeague", null, 10, user, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali");
        leagueDAO.insertLeague(league);
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE messaggi");
            stmt.execute("DROP TABLE utenti_leghe");
            stmt.execute("DROP TABLE regole");
            stmt.execute("DROP TABLE leghe");
            stmt.execute("DROP TABLE utenti");
        }
    }

    @Test
    public void testInsertAndRetrieveMessages() {
        Message msg = new Message("Hello World", user, league.getId());
        boolean inserted = messageDAO.insertMessage(msg);
        assertTrue(inserted);
        
        List<Message> chat = messageDAO.getMessagesByLeagueId(league.getId());
        assertEquals(1, chat.size());
        assertEquals("Hello World", chat.get(0).getText());
        assertEquals("Chatter", chat.get(0).getSender().getUsername());
    }
}