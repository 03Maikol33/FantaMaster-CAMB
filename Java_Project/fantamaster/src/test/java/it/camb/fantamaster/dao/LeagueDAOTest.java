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

public class LeagueDAOTest {

    private LeagueDAO leagueDAO;
    private UserDAO userDAO;
    private Connection connection;
    private User creator;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        // Pulizia totale ad ogni test per evitare conflitti di ID o dati
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Schema completo necessario per LeagueDAO
            stmt.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN DEFAULT FALSE, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), mercato_aperto BOOLEAN DEFAULT FALSE, asta_aperta BOOLEAN DEFAULT TRUE, turno_asta_utente_id INT, giocatore_chiamato_id INT)");
            stmt.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            stmt.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE offerte_asta (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, giocatore_id INT, rosa_id INT, tipo VARCHAR(20), offerta INT)");
        }
        
        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        creator = new User();
        creator.setUsername("Boss");
        creator.setEmail("boss@test.it");
        userDAO.insert(creator);
    }

    // Verifica l'inserimento di una lega con icona e il mapping completo dei campi recuperati.
    // Verifica l'inserimento di una lega con immagine e il mapping completo dalla response SQL.
    @Test
    public void testInsertWithImageAndFullMapping() {
        League league = new League();
        league.setName("Lega Premium");
        league.setCreator(creator);
        league.setCreatedAt(LocalDateTime.now());
        league.setMaxMembers(10);
        league.setGameMode("Scontri Diretti"); // Copre il ramo case-insensitive
        league.setImage(new byte[]{0, 1, 2, 3}); // Copre il ramo Blob/Image

        assertTrue(leagueDAO.insertLeague(league));

        // Test recupero e mapping completo (inclusi wasNull e budget)
        League retrieved = leagueDAO.getLeagueById(league.getId());
        assertNotNull(retrieved);
        assertArrayEquals(new byte[]{0, 1, 2, 3}, retrieved.getImage());
        assertEquals("scontri_diretti", retrieved.getGameMode());
        assertNull(retrieved.getTurnoAstaUtenteId()); // Copre rs.wasNull()
        assertEquals(500, retrieved.getInitialBudget()); // Copre il default budget
    }

    // Verifica i metodi di ricerca: codice invito, leghe create e leghe di cui l'utente fa parte.
    // Verifica i metodi di ricerca: per codice invito, per creatore, per partecipante.
    @Test
    public void testSearchMethods() {
        League l = new League(0, "SearchLeague", null, 8, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", true);
        leagueDAO.insertLeague(l);

        // Test findLeagueByInviteCode
        League byCode = leagueDAO.findLeagueByInviteCode(l.getInviteCode());
        assertNotNull(byCode);
        assertEquals(l.getId(), byCode.getId());

        // Test getLeaguesCreatedByUser
        List<League> created = leagueDAO.getLeaguesCreatedByUser(creator);
        assertEquals(1, created.size());
        
        // Test getLeaguesForUser (richiede relazione in utenti_leghe)
        List<League> memberOf = leagueDAO.getLeaguesForUser(creator);
        assertEquals(1, memberOf.size());
    }

    // Verifica l'aggiornamento dello stato del mercato, delle regole/moduli e la chiusura delle iscrizioni.
    // Verifica gli aggiornamenti dei vari stati: mercato, moduli consentiti, chiusura iscrizioni.
    @Test
    public void testUpdateStatusesAndRules() {
        League l = new League(0, "StatusLeague", null, 8, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", true);
        leagueDAO.insertLeague(l);

        // Test Mercato
        assertFalse(leagueDAO.isMercatoAperto(l.getId()));
        assertTrue(leagueDAO.updateMercato(true, l.getId()));
        assertTrue(leagueDAO.isMercatoAperto(l.getId()));

        // Test Regole/Moduli
        assertTrue(leagueDAO.updateLeagueRules(l.getId(), "3-4-3,4-3-3"));
        assertEquals("3-4-3,4-3-3", leagueDAO.getLeagueById(l.getId()).getAllowedFormations());

        // Test Chiusura Iscrizioni
        assertTrue(leagueDAO.closeRegistrations(l.getId()));
        assertTrue(leagueDAO.getLeagueById(l.getId()).isRegistrationsClosed());
    }

    // Verifica la logica d'asta e la corretta gestione di valori nulli nei campi turno e giocatore.
    // Verifica la logica dell'asta: aggiornamento turno, gestione di NULL, e avvio asta a busta chiusa.
    @Test
    public void testAuctionLogicAndNulls() {
        League l = new League(0, "AuctionLeague", null, 8, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", true);
        leagueDAO.insertLeague(l);

        // Test updateTurnoAsta con Valori
        assertTrue(leagueDAO.updateTurnoAsta(l.getId(), creator.getId(), 101));
        League check = leagueDAO.getLeagueById(l.getId());
        assertEquals((Integer)creator.getId(), check.getTurnoAstaUtenteId());
        assertEquals((Integer)101, check.getGiocatoreChiamatoId());

        // Test updateTurnoAsta con NULL (Copre stmt.setNull)
        assertTrue(leagueDAO.updateTurnoAsta(l.getId(), null, null));
        League checkNull = leagueDAO.getLeagueById(l.getId());
        assertNull(checkNull.getTurnoAstaUtenteId());

        // Test Asta Busta Chiusa (Transazionale)
        assertTrue(leagueDAO.avviaAstaBustaChiusa(l.getId(), 202, 1, 50));
    }

    // Verifica l'eliminazione di una lega tramite l'overload che accetta l'oggetto `League`.
    // Verifica l'overload della cancellazione che accetta direttamente l'oggetto League.
    @Test
    public void testDeleteOverload() {
        League l = new League(0, "ToDelete", null, 8, creator, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", true);
        leagueDAO.insertLeague(l);
        
        // Test l'overload che accetta l'oggetto League
        assertTrue(leagueDAO.deleteLeague(l));
        assertNull(leagueDAO.getLeagueById(l.getId()));
    }

    // Verifica la gestione delle eccezioni SQL forzando lo stato di connessione chiusa.
    // Verifica la gestione degli errori SQL quando la connessione è chiusa.
    @Test
    public void testSqlExceptions() throws SQLException {
        // Forza l'entrata nei blocchi catch chiudendo la connessione
        connection.close();

        assertFalse(leagueDAO.isMercatoAperto(1));
        assertFalse(leagueDAO.updateMercato(true, 1));
        assertFalse(leagueDAO.updateLeagueRules(1, ""));
        assertTrue(leagueDAO.getLeaguesForUser(creator).isEmpty());
        assertTrue(leagueDAO.getLeaguesCreatedByUser(creator).isEmpty());
        assertNull(leagueDAO.getLeagueById(1));
        assertFalse(leagueDAO.insertLeague(new League()));
        assertFalse(leagueDAO.closeRegistrations(1));
        assertFalse(leagueDAO.deleteLeague(1));
        assertNull(leagueDAO.findLeagueByInviteCode("X"));
        assertFalse(leagueDAO.updateTurnoAsta(1, null, null));
        assertFalse(leagueDAO.avviaAstaBustaChiusa(1, 1, 1, 1));
    }
}