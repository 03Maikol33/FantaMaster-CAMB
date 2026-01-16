package it.camb.fantamaster.util;

import it.camb.fantamaster.dao.*;
import it.camb.fantamaster.model.*;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Test per la classe DataSimulator.
 * Verifica che la simulazione di una giornata funzioni correttamente.
 * Introduce dei test di integrazione.
 */

public class DataSimulatorTest {

    private Connection conn;
    private User testUser;
    private League testLeague;

    @Before
    public void setUp() throws Exception {
        conn = ConnectionFactory.getConnection();
        initDatabase();

        // Setup dati minimi per la simulazione
        UserDAO userDAO = new UserDAO(conn);
        testUser = new User();
        testUser.setUsername("SimulatoreTester");
        testUser.setEmail("sim@test.it");
        testUser.setHashPassword("pass");
        userDAO.insert(testUser);

        LeagueDAO leagueDAO = new LeagueDAO(conn);
        testLeague = new League(0, "LegaSim", null, 10, testUser, LocalDateTime.now(), false, new ArrayList<>(), "punti_totali", false);
        leagueDAO.insertLeague(testLeague);
    }

    private void initDatabase() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Creiamo le tabelle necessarie (v5.5 abbreviata per il test)
            st.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100), hash_password VARCHAR(255))");
            st.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100), id_creatore INT, giocatore_chiamato_id INT, turno_asta_utente_id INT)");
            st.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500, bonus_gol DECIMAL(4,1) DEFAULT 3.0, bonus_assist DECIMAL(4,1) DEFAULT 1.0, malus_ammonizione DECIMAL(4,1) DEFAULT 0.5)");
            st.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(100), punteggio_totale DECIMAL(10,2) DEFAULT 0.0)");
            st.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, id_esterno INT UNIQUE, nome VARCHAR(100), ruolo VARCHAR(5), squadra_reale VARCHAR(50))");
            st.execute("CREATE TABLE giocatori_rose (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giocatore_id INT)");
            st.execute("CREATE TABLE formazioni (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giornata_id INT, totale_fantapunti DECIMAL(10,2) DEFAULT 0.0)");
            st.execute("CREATE TABLE dettaglio_formazione (id INT AUTO_INCREMENT PRIMARY KEY, formazione_id INT, giocatore_id INT, fantavoto DECIMAL(4,1) DEFAULT 0.0)");
        }
    }

    /*@Test
    public void testSimulateDayForUser_Success() throws Exception {
        // 1. Prepariamo una rosa con 11 giocatori (usiamo Lautaro ID 17 che è nel JSON)
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, " + testUser.getId() + ", " + testLeague.getId() + ")");
            st.execute("INSERT INTO rosa (id, utenti_leghe_id, nome_rosa) VALUES (1, 1, 'TestRosa')");
            
            // Inseriamo 11 giocatori "fisici" nel DB (ID esterno 17 è Lautaro, giornata 1)
            for (int i = 1; i <= 11; i++) {
                // Il primo è Lautaro (ID 17), gli altri sono dummy per riempire la formazione
                int idEsterno = (i == 1) ? 17 : (100 + i); 
                st.execute("INSERT INTO giocatori (id, id_esterno, nome, ruolo, squadra_reale) VALUES (" + i + ", " + idEsterno + ", 'Player" + i + "', 'A', 'Inter')");
                st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, " + i + ")");
            }
        }

        // 2. Eseguiamo la simulazione (Giornata 1)
        DataSimulator.simulateDayForUser(testUser, testLeague, 1);

        // 3. Verifichiamo che la formazione sia stata creata e i voti salvati
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT totale_fantapunti FROM formazioni WHERE rosa_id = 1 AND giornata_id = 1")) {
            assertTrue("Dovrebbe esistere una formazione simulata", rs.next());
            // Se Lautaro ha preso voto, il totale deve essere diverso da 0
            // Nota: dato che gli altri 10 non hanno voti nel JSON, il totale sarà il voto di Lautaro
            double totale = rs.getDouble(1);
            System.out.println("Totale simulato: " + totale);
        }
    }*/

    @Test
    public void testSimulateDayForUser_NoRosa() {
        // Nessun inserimento in utenti_leghe o rosa
        // Non dovrebbe crashare ma stampare il messaggio di errore nel log
        DataSimulator.simulateDayForUser(testUser, testLeague, 1);
    }
}