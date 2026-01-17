package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.*;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PlayerDAOTest {

    private PlayerDAO playerDAO;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = ConnectionFactory.getConnection();
        playerDAO = new PlayerDAO(conn);

        // 1. CREAZIONE SCHEMA (Indispensabile per H2)
        initDatabase();

        // 2. SETUP UTENTE E SESSIONE
        User testUser = new User();
        testUser.setId(1);
        testUser.setUsername("tester");
        testUser.setEmail("test@test.it");
        
        // Correzione: metodo reale della tua classe SessionUtil
        SessionUtil.createSession(testUser);

        // 3. INSERIMENTO DATI DI TEST
        try (Statement st = conn.createStatement()) {
            // Setup Lega
            st.execute("INSERT INTO leghe (id, nome, max_membri, id_creatore) VALUES (1, 'Lega Test', 10, 1)");
            
            // Setup Giornata
            st.execute("INSERT INTO giornate (id, numero_giornata, data_inizio, stato) VALUES (1, 1, CURRENT_TIMESTAMP, 'da_giocare')");
            
            // Setup Utente-Lega e Rosa
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, 1, 1)");
            st.execute("INSERT INTO rosa (id, utenti_leghe_id, nome_rosa, crediti_residui) VALUES (1, 1, 'Rosa Tester', 500)");

            // Setup Giocatori fisici nel DB
            st.execute("INSERT INTO giocatori (id, id_esterno, nome, ruolo, squadra_reale) VALUES (1, 1, 'Lautaro Martinez', 'A', 'Inter')");
            st.execute("INSERT INTO giocatori (id, id_esterno, nome, ruolo, squadra_reale) VALUES (2, 2, 'Nicolo Barella', 'C', 'Inter')");
            
            // Assegnazione giocatore alla rosa
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 1)");
        }
    }

    /**
     * Esegue i comandi minimi di creazione tabelle per far funzionare i test.
     * H2 in memoria si cancella ad ogni esecuzione.
     */
    private void initDatabase() throws Exception {
        try (Statement st = conn.createStatement()) {
            // Disabilitiamo i vincoli per la pulizia
            st.execute("SET REFERENTIAL_INTEGRITY FALSE"); 
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Creiamo le tabelle necessarie ai test di PlayerDAO
            st.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100) UNIQUE, hash_password VARCHAR(255), avatar mediumblob)");
            st.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, id_esterno INT UNIQUE, nome VARCHAR(100), squadra_reale VARCHAR(50), ruolo VARCHAR(5), quotazione_iniziale INT DEFAULT 1, attivo BOOLEAN DEFAULT TRUE)");
            st.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100), max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN DEFAULT FALSE, codice_invito VARCHAR(10) UNIQUE, mercato_aperto BOOLEAN DEFAULT FALSE, asta_aperta BOOLEAN DEFAULT TRUE, turno_asta_utente_id INT, giocatore_chiamato_id INT)");
            st.execute("CREATE TABLE giornate (id INT AUTO_INCREMENT PRIMARY KEY, numero_giornata INT, data_inizio TIMESTAMP, stato VARCHAR(20))");
            st.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, UNIQUE(utente_id, lega_id))");
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(100), crediti_residui INT, punteggio_totale DECIMAL(5,1), UNIQUE(utenti_leghe_id))");
            st.execute("CREATE TABLE giocatori_rose (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giocatore_id INT, costo_acquisto INT DEFAULT 1, UNIQUE(rosa_id, giocatore_id))");
            st.execute("CREATE TABLE formazioni (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giornata_id INT, modulo_schierato VARCHAR(10), capitano_id INT, id_utente INT, id_lega INT)");
            st.execute("CREATE TABLE dettaglio_formazione (id INT AUTO_INCREMENT PRIMARY KEY, formazione_id INT, giocatore_id INT, stato VARCHAR(20),fantavoto DECIMAL(4,1) DEFAULT 0.0, ordine_panchina INT, fantavoto_calcolato DECIMAL(4,1) DEFAULT 0.0)");
        }
    }

    // ==========================================================
    // TEST LOGICA JSON (Listone)
    // ==========================================================

    // Verifica il caricamento di tutti i giocatori dal listone JSON.
    @Test
    public void testGetAllPlayers() {
        List<Player> players = playerDAO.getAllPlayers();
        assertNotNull(players);
        assertFalse(players.isEmpty());
    }

    // Verifica la paginazione: offset e limite restituiscono il numero corretto di giocatori.
    @Test
    public void testPagination() {
        // Offset 0, limite 5
        List<Player> page = playerDAO.getPlayersPage(0, 5);
        assertEquals(5, page.size());

        // Offset altissimo -> lista vuota
        List<Player> empty = playerDAO.getPlayersPage(10000, 5);
        assertTrue(empty.isEmpty());
    }

    // Verifica i filtri: per ruolo, prezzo minimo/massimo, e il filtro 'Tutti'.
    @Test
    public void testFilteringCompleto() {
        // Filtro Ruolo "A" e prezzo tra 20 e 50
        List<Player> filtered = playerDAO.getPlayersPageFiltered(0, 100, "A", 20, 50);
        for (Player p : filtered) {
            assertEquals("A", p.getRuolo());
            assertTrue(p.getPrezzo() >= 20 && p.getPrezzo() <= 50);
        }

        // Filtro "Tutti" e solo prezzo minimo
        List<Player> allWithMinPrice = playerDAO.getPlayersPageFiltered(0, 100, "Tutti", 40, null);
        for (Player p : allWithMinPrice) {
            assertTrue(p.getPrezzo() >= 40);
        }
    }

    // Verifica il conteggio corretto dei giocatori filtrati.
    @Test
    public void testGetFilteredTotalCount() {
        int countP = playerDAO.getFilteredTotalCount("P", null, null);
        int countAll = playerDAO.getTotalCount();
        assertTrue(countP > 0 && countP < countAll);
    }

    // Verifica la ricerca di un giocatore per ID e la gestione di ID inesistenti.
    @Test
    public void testGetPlayerById() {
        // Il giocatore con ID 1 esiste nel listone.json (solitamente è un portiere o il primo in ordine)
        Player p = playerDAO.getPlayerById(1);
        assertNotNull(p);
        assertEquals(1, p.getId());

        assertNull(playerDAO.getPlayerById(-999));
    }

    // ==========================================================
    // TEST LOGICA DATABASE (JDBC)
    // ==========================================================

    // Verifica il recupero della rosa di una squadra in una lega specifica.
    @Test
    public void testGetTeamRosterByLeague() {
        // Abbiamo inserito Lautaro (ID_ESTERN 1) nella 'Lega Test' nel setup
        List<Player> roster = playerDAO.getTeamRosterByLeague("Lega Test");
        assertNotNull(roster);
        assertFalse(roster.isEmpty());
        // Verifichiamo che il giocatore recuperato sia quello corretto dal JSON
        assertEquals(1, roster.get(0).getId());
    }

    // Verifica il salvataggio di una formazione completa (titolari + panchina) e il recupero dei dettagli.
    @Test
    public void testSaveFormationAndGetDetails() throws Exception {
        Player cap = playerDAO.getPlayerById(1); // Lautaro
        List<Player> titolari = new ArrayList<>();
        titolari.add(cap);
        List<Player> panchina = new ArrayList<>();
        panchina.add(playerDAO.getPlayerById(2)); // Barella

        // userId=1, leagueId=1, modulo 4-4-2
        boolean saved = playerDAO.saveFormation(1, 1, "4-4-2", cap, titolari, panchina);
        assertTrue("La formazione dovrebbe essere salvata", saved);

        // Verifichiamo il recupero tramite formationId (che nel DB H2 sarà 1 essendo il primo)
        List<Player> schierati = playerDAO.getPlayersByFormationId(1);
        assertEquals(2, schierati.size()); // 1 titolare + 1 panchinaro
    }

    // Verifica l'aggiornamento dei voti calcolati e il calcolo del punteggio totale della squadra.
    @Test
    public void testUpdateCalculatedVotes() throws Exception {
        // Prepariamo una formazione esistente nel DB
        testSaveFormationAndGetDetails();

        // Prepariamo i dati dei voti calcolati
        List<FormationPlayer> fpList = new ArrayList<>();
        Player p = playerDAO.getPlayerById(1);
        FormationPlayer fp = new FormationPlayer(p, true, 0, true);
        fp.setFantavotoCalcolato(8.5);
        fpList.add(fp);

        // Chiamata al metodo (userId=1, leagueId=1, giornata=1)
        playerDAO.updateCalculatedVotes(1, 1, 1, fpList);

        // Verifichiamo che il totale venga calcolato correttamente dal DB
        double totale = playerDAO.getTeamTotalScore(1, 1, 1);
        assertEquals(8.5, totale, 0.01);
    }

    // Verifica il recupero dei giocatori appartenenti a una rosa specifica.
    @Test
    public void testGetPlayersByRosa() {
        // Rosa con ID 1 nel setup
        List<Player> players = playerDAO.getPlayersByRosa(1);
        assertEquals(1, players.size());
        assertEquals(1, players.get(0).getId());
    }
}