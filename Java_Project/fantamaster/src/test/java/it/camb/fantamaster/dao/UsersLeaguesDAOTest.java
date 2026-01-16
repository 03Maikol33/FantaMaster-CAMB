package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        initDatabase();

        ulDAO = new UsersLeaguesDAO(connection);
        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        // Setup dati base
        user = new User();
        user.setUsername("Maikol");
        user.setEmail("maikol@test.it");
        user.setHashPassword("x");
        userDAO.insert(user);

        User creator = new User();
        creator.setUsername("Admin");
        creator.setEmail("admin@test.it");
        creator.setHashPassword("x");
        userDAO.insert(creator);

        league = new League(0, "Lega Test", null, 10, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", false);
        leagueDAO.insertLeague(league);
    }

    private void initDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");

            stmt.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255) UNIQUE, hash_password VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, modalita VARCHAR(50), asta_aperta BOOLEAN, mercato_aperto BOOLEAN, icona BLOB, max_membri INT, codice_invito VARCHAR(255), moduli_consentiti VARCHAR(255))");
            stmt.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, UNIQUE(utente_id, lega_id))");
            stmt.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT)");
            stmt.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), punteggio_totale DOUBLE DEFAULT 0.0, crediti_residui INT)");
            stmt.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, id_esterno INT, nome VARCHAR(255), ruolo VARCHAR(10), squadra_reale VARCHAR(100))");
            stmt.execute("CREATE TABLE giornate (id INT AUTO_INCREMENT PRIMARY KEY, numero_giornata INT, data_inizio TIMESTAMP, stato VARCHAR(50))");
            stmt.execute("CREATE TABLE formazioni (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giornata_id INT, modulo_schierato VARCHAR(10), totale_fantapunti DOUBLE)");
            stmt.execute("CREATE TABLE dettaglio_formazione (id INT AUTO_INCREMENT PRIMARY KEY, formazione_id INT, giocatore_id INT, fantavoto DOUBLE, stato VARCHAR(20))");
        }
    }

    @Test
    public void testSubscriptionAndDuplicates() throws SQLException {
        // Test iscrizione singola
        int subId = ulDAO.subscribeUserToLeague(user, league);
        assertTrue(subId > 0);

        // Test iscrizione DUPLICATA (per coprire SQLIntegrityConstraintViolationException e getExistingSubscriptionId)
        int duplicateId = ulDAO.subscribeUserToLeague(user, league);
        assertEquals("Dovrebbe restituire lo stesso ID iscrizione", subId, duplicateId);

        // Test isUserSubscribed e getUsersInLeague
        assertTrue(ulDAO.isUserSubscribed(user, league));
        assertEquals(2, ulDAO.getUsersInLeague(league).size()); // Include anche il creatore
        assertEquals(2, ulDAO.getParticipants(league.getId()).size()); // Include anche il creatore
    }

    @Test
    public void testRankingAndHistory() throws SQLException {
        // 1. Setup dati per classifica
        int subId = ulDAO.subscribeUserToLeague(user, league);
        try (Statement st = connection.createStatement()) {
            st.execute("INSERT INTO rosa (id, utenti_leghe_id, nome_rosa, punteggio_totale) VALUES (1, " + subId + ", 'Maikol Team', 85.5)");
            st.execute("INSERT INTO giocatori (id, nome, ruolo) VALUES (1, 'Lautaro', 'A')");
            st.execute("INSERT INTO giornate (id, numero_giornata) VALUES (1, 1)");
            st.execute("INSERT INTO formazioni (id, rosa_id, giornata_id) VALUES (1, 1, 1)");
            st.execute("INSERT INTO dettaglio_formazione (formazione_id, giocatore_id, fantavoto, stato) VALUES (1, 1, 8.5, 'titolare')");
        }

        // 2. Test Ranking
        ulDAO.updateLeagueRanking(league.getId());
        List<UsersLeaguesDAO.UserRankingRow> ranking = ulDAO.getLeagueRanking(league.getId());
        assertFalse(ranking.isEmpty());
        
        // Copertura Getter classe interna UserRankingRow
        UsersLeaguesDAO.UserRankingRow row = ranking.get(0);
        assertEquals(1, row.getPosizione());
        assertEquals("Maikol", row.getUsername());
        assertEquals("Maikol Team", row.getNomeSquadra());
        assertTrue(row.getPunteggio() >= 0);

        // 3. Test Storico Giornate e Punteggi Formazione
        List<Integer> matchdays = ulDAO.getPlayedMatchdays(user.getId(), league.getId());
        assertEquals(1, matchdays.size());

        List<UsersLeaguesDAO.PlayerScoreRow> scores = ulDAO.getFormationScores(user.getId(), league.getId(), 1);
        assertFalse(scores.isEmpty());
        
        // Copertura Getter classe interna PlayerScoreRow
        UsersLeaguesDAO.PlayerScoreRow scoreRow = scores.get(0);
        assertEquals("Lautaro", scoreRow.getNome());
        assertEquals("A", scoreRow.getRuolo());
        assertEquals(8.5, scoreRow.getFantavoto(), 0.01);
        assertTrue(scoreRow.isTitolare());
    }

    @Test
    public void testSimulatoreHelpers() throws SQLException {
        int subId = ulDAO.subscribeUserToLeague(user, league);
        try (Statement st = connection.createStatement()) {
            st.execute("INSERT INTO rosa (id, utenti_leghe_id) VALUES (1, " + subId + ")");
        }

        // Test getRosaId
        assertEquals(1, ulDAO.getRosaId(user.getId(), league.getId()));
        assertEquals(-1, ulDAO.getRosaId(-99, -99));

        // Test createDummyFormation e helper Giornate
        int formId = ulDAO.createDummyFormation(1, 10); // Giornata 10 non esiste
        assertTrue(formId > 0);
        
        // Test salvataggio voto (ON DUPLICATE KEY)
        ulDAO.savePlayerScore(formId, 1, 7.5, true);
        ulDAO.savePlayerScore(formId, 1, 8.0, true); // Update dello stesso voto
    }

    @Test
    public void testExceptionHandling() throws SQLException {
        // Chiudiamo la connessione per forzare le SQLException in tutti i rami catch
        connection.close();

        // Chiamate che devono restituire valori di default in caso di errore
        assertFalse(ulDAO.isUserSubscribed(user, league));
        assertFalse(ulDAO.unsubscribeUserFromLeague(user, league));
        assertTrue(ulDAO.getUsersInLeagueId(1).isEmpty());
        assertTrue(ulDAO.getLeagueRanking(1).isEmpty());
        assertTrue(ulDAO.getPlayedMatchdays(1, 1).isEmpty());
        assertTrue(ulDAO.getFormationScores(1, 1, 1).isEmpty());
        assertEquals(-1, ulDAO.getRosaId(1, 1));
    }
}