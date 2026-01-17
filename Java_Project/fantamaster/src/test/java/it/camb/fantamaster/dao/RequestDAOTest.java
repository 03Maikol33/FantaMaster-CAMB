package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.RequestStatus;
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
    
    private User joiner;
    private League league;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        initDatabase();

        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);
        requestDAO = new RequestDAO(connection);

        // Utente richiedente
        joiner = new User(); 
        joiner.setUsername("Joiner"); 
        joiner.setEmail("joiner@test.com"); 
        joiner.setHashPassword("x");
        userDAO.insert(joiner);
        
        // Creatore
        User creator = new User(); 
        creator.setUsername("Creator"); 
        creator.setEmail("creator@test.com"); 
        creator.setHashPassword("x");
        userDAO.insert(creator);

        // Lega
        league = new League(0, "Liga", null, 10, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", false);
        leagueDAO.insertLeague(league);
    }

    private void initDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");

            stmt.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255) UNIQUE, hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), asta_aperta BOOLEAN DEFAULT FALSE)");
            stmt.execute("CREATE TABLE richieste_accesso (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, stato VARCHAR(50), data_richiesta TIMESTAMP)");
            stmt.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            stmt.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), punteggio_totale DOUBLE DEFAULT 0.0, crediti_residui INT DEFAULT 500)");
            stmt.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
        }
    }

    // Verifica la creazione di richieste e che i duplicati vengano rifiutati.
    @Test
    public void testCreateRequestAndDuplicates() {
        // Primo inserimento: successo
        assertTrue(requestDAO.createRequest(joiner, league));
        
        // Controllo pendenti (copre hasPendingRequest)
        assertTrue(requestDAO.hasPendingRequest(joiner, league));

        // Secondo inserimento (DUPLICATO): deve restituire false
        // Questo copre il ramo "if (hasPendingRequest(user, league)) { return false; }"
        assertFalse("Non dovrebbe permettere richieste duplicate", requestDAO.createRequest(joiner, league));
    }

    // Verifica il rifiuto di una richiesta e la sua rimozione dal DB.
    @Test
    public void testRejectRequest() {
        requestDAO.createRequest(joiner, league);
        
        // Azione: Rifiuto (chiama deleteRequest internamente)
        boolean rejected = requestDAO.rejectRequest(joiner, league);
        assertTrue(rejected);
        
        // Verifica: non ci sono più richieste
        assertFalse(requestDAO.hasPendingRequest(joiner, league));
    }

    // Verifica i corner case di mapping, incluso il timestamp NULL dal DB.
    @Test
    public void testMappingCornerCases() throws SQLException {
        try (Statement st = connection.createStatement()) {
            //data a NULL per coprire il ramo (ts != null)
            st.execute("INSERT INTO richieste_accesso (utente_id, lega_id, stato, data_richiesta) " +
                    "VALUES (" + joiner.getId() + ", " + league.getId() + ", 'in_attesa', NULL)");
        }

        List<Request> list = requestDAO.getRequestsForLeague(league);

        assertFalse("La lista dovrebbe contenere la richiesta appena inserita", list.isEmpty());
        
        Request req = list.get(0);
        
        // 1. Verifichiamo la copertura del ramo Timestamp NULL
        assertNull("Il timestamp dovrebbe essere null come inserito nel DB", req.getTimestamp());
    }

    // Verifica che l'approvazione di una richiesta inesistente fallisca correttamente.
    @Test
    public void testApproveRequestFailures() throws SQLException {
        // Caso 1: Approvazione di una richiesta che non esiste nel DB
        // (Copre il ramo: if (stmt.executeUpdate() == 0) { conn.rollback(); return false; })
        boolean result = requestDAO.approveRequest(joiner, league);
        assertFalse("Approvazione dovrebbe fallire se la richiesta non esiste", result);

        // Caso 2: utentiLegheId == -1 (Simulato chiudendo la connessione o forzando errore)
        // Questo è coperto indirettamente dal test testSqlExceptions sotto
    }

    // Verifica la gestione degli errori SQL quando la connessione è chiusa.
    @Test
    public void testSqlExceptions() throws SQLException {
        // Chiusura connessione per forzare TUTTI i blocchi catch (SQLException)
        connection.close();

        assertFalse(requestDAO.createRequest(joiner, league));
        assertTrue(requestDAO.getRequestsForLeague(league).isEmpty());
        assertFalse(requestDAO.hasPendingRequest(joiner, league));
        assertFalse(requestDAO.deleteRequest(joiner, league));
        assertFalse(requestDAO.approveRequest(joiner, league));
    }
}