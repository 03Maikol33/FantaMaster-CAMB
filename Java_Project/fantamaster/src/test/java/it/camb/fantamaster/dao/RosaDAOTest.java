package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.util.ConnectionFactory;
import jakarta.persistence.criteria.CriteriaBuilder.In;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RosaDAOTest {

    private RosaDAO rosaDAO;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = ConnectionFactory.getConnection();
        rosaDAO = new RosaDAO(conn);
        initDatabase();
    }

    private void initDatabase() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Tabelle allineate al codice di RosaDAO
            st.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY)");
            st.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY)");
            st.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            
            // Nota: uso 'crediti_residui' perché il tuo DAO fa rs.getInt(\"crediti_residui\")
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(100), crediti_residui INT DEFAULT 500)");
            
            st.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100), ruolo VARCHAR(5), squadra_reale VARCHAR(50), quotazione_iniziale INT)");
            st.execute("CREATE TABLE giocatori_rose (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giocatore_id INT)");
        }
    }

    @Test
    public void testGetRosaByUserAndLeague() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, 10, 20)");
            st.execute("INSERT INTO rosa (id, utenti_leghe_id, nome_rosa, crediti_residui) VALUES (1, 1, 'Bassma Team', 450)");
        }

        Rosa rosa = rosaDAO.getRosaByUserAndLeague(10, 20);
        assertNotNull(rosa);
        assertEquals("Bassma Team", rosa.getNomeRosa());
        assertEquals(450, rosa.getCreditiDisponibili());
    }

    @Test
    public void testCountGiocatori() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 101)");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 102)");
        }

        assertEquals(2, rosaDAO.countGiocatoriInRosa(1));
    }

    @Test
    public void testCountGiocatoriPerRuolo() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO giocatori (id, ruolo) VALUES (1, 'A')");
            st.execute("INSERT INTO giocatori (id, ruolo) VALUES (2, 'A')");
            st.execute("INSERT INTO giocatori (id, ruolo) VALUES (3, 'D')");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 1)");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 2)");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 3)");
        }

        assertEquals(2, rosaDAO.countGiocatoriPerRuolo(1, "A"));
        assertEquals(1, rosaDAO.countGiocatoriPerRuolo(1, "D"));
        assertEquals(0, rosaDAO.countGiocatoriPerRuolo(1, "P"));
    }

    @Test
    public void testGetRuoliCount() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO giocatori (id, ruolo) VALUES (1, 'P')");
            st.execute("INSERT INTO giocatori (id, ruolo) VALUES (2, 'C')");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 1)");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 2)");
        }

        Map<String, Integer> counts = rosaDAO.getRuoliCount(1);
        assertEquals(Integer.valueOf(1), counts.get("P"));
        assertEquals(Integer.valueOf(1), counts.get("C"));
        assertEquals(Integer.valueOf(0), counts.get("A"));
    }

    @Test
    public void testGetRuoliCountRuoloInesistente() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO giocatori (id, ruolo) VALUES (1, 'X')");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 1)");
        }

        Map<String, Integer> counts = rosaDAO.getRuoliCount(1);
        assertEquals(Integer.valueOf(0), counts.get("P"));
        assertEquals(Integer.valueOf(0), counts.get("C"));
        assertEquals(Integer.valueOf(0), counts.get("A"));
        assertEquals(Integer.valueOf(1), counts.get("X")); // Verifica che il ruolo 'X' sia contato correttamente
    }

    @Test
    public void testGetPlayersByRosaId() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO giocatori (id, nome, ruolo, squadra_reale, quotazione_iniziale) " +
                       "VALUES (1, 'Lautaro', 'A', 'Inter', 30)");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 1)");
        }

        List<Player> players = rosaDAO.getPlayersByRosaId(1);
        assertFalse(players.isEmpty());
        assertEquals("Lautaro", players.get(0).getNome());
        assertEquals("Inter", players.get(0).getSquadra());
    }

    @Test
    public void testCreateDefaultRosa() throws Exception {
        boolean created = rosaDAO.createDefaultRosa(1);
        assertTrue(created);

        // Verifica inserimento effettivo
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM rosa WHERE utenti_leghe_id = 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    public void testUpdateRosaInfo() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO rosa (id, nome_rosa) VALUES (1, 'Vecchio Nome')");
        }

        boolean updated = rosaDAO.updateRosaInfo(1, "Nuovo Nome");
        assertTrue(updated);

        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT nome_rosa FROM rosa WHERE id = 1")) {
            assertTrue(rs.next());
            assertEquals("Nuovo Nome", rs.getString("nome_rosa"));
        }
    }

    @Test
    public void testGetIdsGiocatoriCompratiInLega() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO utenti_leghe (id, lega_id) VALUES (1, 10)");
            st.execute("INSERT INTO rosa (id, utenti_leghe_id) VALUES (1, 1)");
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 500)");
        }

        List<Integer> ids = rosaDAO.getIdsGiocatoriCompratiInLega(10);
        assertEquals(1, ids.size());
        assertEquals(Integer.valueOf(500), ids.get(0));
    }
}