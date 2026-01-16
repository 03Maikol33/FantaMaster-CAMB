package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.Scambio;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class ScambiDAOTest {

    private ScambiDAO scambiDAO;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = ConnectionFactory.getConnection();
        scambiDAO = new ScambiDAO(conn);
        initDatabase();
    }

    private void initDatabase() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Tabelle necessarie per ScambiDAO
            st.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY)");
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, nome_rosa VARCHAR(100))");
            st.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, id_esterno INT UNIQUE, nome VARCHAR(100))");
            st.execute("CREATE TABLE giocatori_rose (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giocatore_id INT)");
            st.execute("CREATE TABLE scambi (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lega_id INT, " +
                    "rosa_richiedente_id INT, " +
                    "rosa_ricevente_id INT, " +
                    "giocatore_offerto_id INT, " +
                    "giocatore_richiesto_id INT, " +
                    "stato VARCHAR(20))");
        }
    }

    @Test
    public void testProponiScambio() throws Exception {
        Scambio s = new Scambio();
        s.setLegaId(1);
        s.setRosaRichiedenteId(10);
        s.setRosaRiceventeId(11);
        s.setGiocatoreOffertoId(101); //sono entrambi due difensori
        s.setGiocatoreRichiestoId(202);//sono entrambi due difensori

        assertTrue(scambiDAO.proponiScambio(s));

        // Verifica inserimento e stato iniziale
        assertEquals(1, scambiDAO.countRichiestePendenti(11));
    }

    @Test
    public void testGetScambiRicevuti() throws Exception {
        // Setup dati per i JOIN
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO rosa (id, nome_rosa) VALUES (1, 'Richiedente Team')");
            st.execute("INSERT INTO giocatori (id, id_esterno, nome) VALUES (10, 100, 'Gioc Offerto')"); //difensore
            st.execute("INSERT INTO giocatori (id, id_esterno, nome) VALUES (20, 102, 'Gioc Richiesto')");//difensore
            // Lo scambio nel DB punta agli ID interni della tabella giocatori
            st.execute("INSERT INTO scambi (id, lega_id, rosa_richiedente_id, rosa_ricevente_id, giocatore_offerto_id, giocatore_richiesto_id, stato) " +
                       "VALUES (1, 1, 1, 2, 10, 20, 'proposto')");
        }

        List<Scambio> ricevuti = scambiDAO.getScambiRicevuti(2);
        assertFalse(ricevuti.isEmpty());
        Scambio s = ricevuti.get(0);
        assertEquals("Richiedente Team", s.getNomeRichiedente());
        assertEquals("Gioc Offerto", s.getNomeGiocatoreOfferto());
        assertEquals(100, s.getGiocatoreOffertoId()); // Verifica mapping id_esterno
    }

    @Test
    public void testRifiutaScambio() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO scambi (id, rosa_ricevente_id, stato) VALUES (1, 2, 'proposto')");
        }

        scambiDAO.rifiutaScambio(1);
        assertEquals(0, scambiDAO.countRichiestePendenti(2));
    }

    @Test
    public void testAccettaScambio() throws Exception {
        // 1. Setup complesso: due rose con un giocatore ciascuna
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO giocatori (id, id_esterno, nome) VALUES (1, 17, 'Lautaro')");
            st.execute("INSERT INTO giocatori (id, id_esterno, nome) VALUES (2, 22, 'Barella')");
            
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (1, 1)"); // Rosa 1 ha Lautaro
            st.execute("INSERT INTO giocatori_rose (rosa_id, giocatore_id) VALUES (2, 2)"); // Rosa 2 ha Barella
            
            st.execute("INSERT INTO scambi (id, rosa_richiedente_id, rosa_ricevente_id, giocatore_offerto_id, giocatore_richiesto_id, stato) " +
                       "VALUES (1, 1, 2, 1, 2, 'proposto')");
        }

        Scambio s = new Scambio();
        s.setId(1);
        s.setRosaRichiedenteId(1);
        s.setRosaRiceventeId(2);
        s.setGiocatoreOffertoId(17); // ID esterno
        s.setGiocatoreRichiestoId(22); // ID esterno

        // 2. Esecuzione
        scambiDAO.accettaScambio(s);

        // 3. Verifiche
        try (Statement st = conn.createStatement()) {
            // Verifica stato scambio
            ResultSet rsS = st.executeQuery("SELECT stato FROM scambi WHERE id = 1");
            assertTrue(rsS.next());
            assertEquals("accettato", rsS.getString("stato"));

            // Verifica spostamento Lautaro (ID 1) alla Rosa 2
            ResultSet rs1 = st.executeQuery("SELECT rosa_id FROM giocatori_rose WHERE giocatore_id = 1");
            assertTrue(rs1.next());
            assertEquals(2, rs1.getInt("rosa_id"));

            // Verifica spostamento Barella (ID 2) alla Rosa 1
            ResultSet rs2 = st.executeQuery("SELECT rosa_id FROM giocatori_rose WHERE giocatore_id = 2");
            assertTrue(rs2.next());
            assertEquals(1, rs2.getInt("rosa_id"));
        }
    }
}