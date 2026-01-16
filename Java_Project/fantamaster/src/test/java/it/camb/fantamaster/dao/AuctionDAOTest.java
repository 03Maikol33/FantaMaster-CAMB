package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.BidResult;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class AuctionDAOTest {

    private AuctionDAO auctionDAO;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = ConnectionFactory.getConnection();
        auctionDAO = new AuctionDAO(conn);
        initDatabase();
    }

    private void initDatabase() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            st.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, giocatore_chiamato_id INT, turno_asta_utente_id INT)");
            // Tabella utenti_leghe necessaria per il link con rosa
            st.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            // Tabella rosa con il vincolo utenti_leghe_id
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, nome_rosa VARCHAR(100), crediti_residui INT, utenti_leghe_id INT NOT NULL)");
            st.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100))");
            st.execute("CREATE TABLE offerte_asta (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, giocatore_id INT, rosa_id INT, offerta INT, tipo VARCHAR(20))");
            st.execute("CREATE TABLE giocatori_rose (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giocatore_id INT, costo_acquisto INT)");
        }
    }

    @Test
    public void testAstaDeserta() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO leghe (id) VALUES (1)");
        }
        String risultato = auctionDAO.chiudiAstaEAssegna(1, 99);
        assertEquals("Nessuno (asta deserta)", risultato);
    }

    @Test
    public void testVincitoreSingolo() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO leghe (id) VALUES (1)");
            st.execute("INSERT INTO giocatori (id, nome) VALUES (10, 'Osimhen')");
            
            // Inseriamo i link utenti_leghe
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, 1, 1)");
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (2, 2, 1)");
            
            // Rose con utenti_leghe_id obbligatorio
            st.execute("INSERT INTO rosa (id, nome_rosa, crediti_residui, utenti_leghe_id) VALUES (1, 'Team A', 500, 1)");
            st.execute("INSERT INTO rosa (id, nome_rosa, crediti_residui, utenti_leghe_id) VALUES (2, 'Team B', 500, 2)");
            
            st.execute("INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, offerta, tipo) VALUES (1, 10, 1, 50, 'offerta')");
            st.execute("INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, offerta, tipo) VALUES (1, 10, 2, 80, 'offerta')");
        }

        String esito = auctionDAO.chiudiAstaEAssegna(1, 10);
        assertTrue(esito.contains("Team B"));
        assertTrue(esito.contains("80"));

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT crediti_residui FROM rosa WHERE id = 2")) {
            assertTrue(rs.next());
            assertEquals(420, rs.getInt(1));
        }
    }

    @Test
    public void testPareggioEAssegnazioneCasuale() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO leghe (id) VALUES (1)");
            
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, 1, 1)");
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (2, 2, 1)");
            
            st.execute("INSERT INTO rosa (id, nome_rosa, crediti_residui, utenti_leghe_id) VALUES (1, 'Team A', 500, 1)");
            st.execute("INSERT INTO rosa (id, nome_rosa, crediti_residui, utenti_leghe_id) VALUES (2, 'Team B', 500, 2)");
            
            st.execute("INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, offerta, tipo) VALUES (1, 10, 1, 100, 'offerta')");
            st.execute("INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, offerta, tipo) VALUES (1, 10, 2, 100, 'offerta')");
        }

        String esito = auctionDAO.chiudiAstaEAssegna(1, 10);
        assertNotNull(esito);
        assertTrue(esito.contains("100"));

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM giocatori_rose WHERE giocatore_id = 10")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    public void testHasUserAlreadyBid() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO offerte_asta (lega_id, giocatore_id, rosa_id, offerta, tipo) VALUES (1, 10, 1, 50, 'offerta')");
        }

        assertTrue(auctionDAO.hasUserAlreadyBid(1, 10, 1));
        assertFalse(auctionDAO.hasUserAlreadyBid(1, 10, 2));
    }

    @Test
    public void testGetUltimoRisultatoAsta() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, 1, 1)");
            // Link utenti_leghe_id = 1
            st.execute("INSERT INTO rosa (id, nome_rosa, utenti_leghe_id) VALUES (1, 'MyTeam', 1)");
            st.execute("INSERT INTO giocatori (id, nome) VALUES (5, 'Dybala')");
            st.execute("INSERT INTO giocatori_rose (id, rosa_id, giocatore_id, costo_acquisto) VALUES (1, 1, 5, 45)");
        }

        String risultato = auctionDAO.getUltimoRisultatoAsta(1);
        assertEquals("Dybala assegnato a MyTeam per 45 FM", risultato);
    }
}