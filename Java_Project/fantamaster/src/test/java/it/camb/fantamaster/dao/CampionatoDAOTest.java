package it.camb.fantamaster.dao;

import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class CampionatoDAOTest {

    private CampionatoDAO campionatoDAO;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = ConnectionFactory.getConnection();
        campionatoDAO = new CampionatoDAO(conn);
        initDatabase();
    }

    private void initDatabase() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Tabelle minime necessarie per CampionatoDAO
            st.execute("CREATE TABLE stato_campionato (id INT PRIMARY KEY, giornata_corrente INT, ultima_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE giornate (id INT AUTO_INCREMENT PRIMARY KEY, numero_giornata INT, data_inizio TIMESTAMP, stato VARCHAR(20))");
            st.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100), id_creatore INT)");
            st.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, bonus_gol DECIMAL(4,1), bonus_assist DECIMAL(4,1), bonus_imbattibilita DECIMAL(4,1), bonus_rigore_parato DECIMAL(4,1), bonus_fattore_campo DECIMAL(4,1), malus_gol_subito DECIMAL(4,1), malus_ammonizione DECIMAL(4,1), malus_espulsione DECIMAL(4,1), malus_rigore_sbagliato DECIMAL(4,1), malus_autogol DECIMAL(4,1), budget_iniziale INT, voto_base DECIMAL(4,1), usa_modificatore_difesa BOOLEAN DEFAULT FALSE)");
            st.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100), hash_password VARCHAR(255))");
            st.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, punteggio_totale DECIMAL(5,1) DEFAULT 0.0)");
            st.execute("CREATE TABLE giocatori (id INT AUTO_INCREMENT PRIMARY KEY, id_esterno INT UNIQUE, nome VARCHAR(100), ruolo VARCHAR(5), squadra_reale VARCHAR(50))");
            st.execute("CREATE TABLE giocatori_rose (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giocatore_id INT)");
            st.execute("CREATE TABLE formazioni (id INT AUTO_INCREMENT PRIMARY KEY, rosa_id INT, giornata_id INT, totale_fantapunti DECIMAL(5,1) DEFAULT 0.0, capitano_id INT, modulo_schierato VARCHAR(10))");
            st.execute("CREATE TABLE dettaglio_formazione (id INT AUTO_INCREMENT PRIMARY KEY, formazione_id INT, giocatore_id INT, fantavoto DECIMAL(4,1) DEFAULT 0.0, stato VARCHAR(20), ordine_panchina INT)");
        }
    }

    @Test
    public void testInizializzazioneERecuperoStato() throws Exception {
        // Test metodo inizializzaCampionato (azzera tutto e mette riga id=1)
        campionatoDAO.inizializzaCampionato();
        
        assertTrue("La riga di stato dovrebbe esistere", campionatoDAO.existsStatoCampionato());
        assertEquals("La giornata corrente iniziale deve essere 0", 0, campionatoDAO.getGiornataCorrente());
    }

    @Test
    public void testProgrammazioneGiornata() throws Exception {
        campionatoDAO.inizializzaCampionato();
        
        // Programmiamo la giornata 1
        campionatoDAO.programmaGiornata(1);
        
        assertTrue("La giornata 1 dovrebbe esistere nel DB", campionatoDAO.existsGiornataFisica(1));
        
        // Verifica stato giornata
        try (Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery("SELECT stato FROM giornate WHERE numero_giornata = 1")) {
            if (rs.next()) {
                assertEquals("da_giocare", rs.getString("stato"));
            }
        }
    }

    @Test
    public void testEseguiGiornataEProgrammaSuccessiva() throws Exception {
        campionatoDAO.inizializzaCampionato();
        campionatoDAO.programmaGiornata(1);
        
        // Chiudiamo la 1 e apriamo la 2
        campionatoDAO.eseguiGiornataEProgrammaSuccessiva(1);
        
        assertEquals("L'ultima conclusa deve essere la 1", 1, campionatoDAO.getGiornataCorrente());
        assertTrue("Dovrebbe essere stata creata la giornata 2", campionatoDAO.existsGiornataFisica(2));
    }

    @Test
    public void testSincronizzaPunteggiLega() throws Exception {
        // 1. Setup dati complesso
        campionatoDAO.inizializzaCampionato();
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO utenti (id, username) VALUES (1, 'user')");
            st.execute("INSERT INTO leghe (id, nome, id_creatore) VALUES (1, 'Lega1', 1)");
            st.execute("INSERT INTO regole (lega_id, bonus_gol, bonus_assist, bonus_imbattibilita, bonus_rigore_parato, bonus_fattore_campo, malus_gol_subito, malus_ammonizione, malus_espulsione, malus_rigore_sbagliato, malus_autogol, budget_iniziale, voto_base, usa_modificatore_difesa) VALUES (1, 3, 1, 1, 3, 2, 1, 0.5, 1, 3, 2, 500, 6, FALSE)");
            st.execute("INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES (1, 1, 1)");
            st.execute("INSERT INTO rosa (id, utenti_leghe_id) VALUES (1, 1)");
            st.execute("INSERT INTO giornate (id, numero_giornata, stato) VALUES (1, 1, 'da_giocare')");
            st.execute("INSERT INTO formazioni (id, rosa_id, giornata_id, totale_fantapunti) VALUES (1, 1, 1, 0.0)");
            
            // Giocatore dell'Inter (sappiamo che c'è nel JSON della giornata 1)
            st.execute("INSERT INTO giocatori (id, id_esterno, nome, ruolo, squadra_reale) VALUES (1, 17, 'Lautaro', 'A', 'Inter')");
            st.execute("INSERT INTO dettaglio_formazione (formazione_id, giocatore_id, stato) VALUES (1, 1, 'titolare')");
            
            // Avanziamo il campionato alla giornata 1 conclusa
            st.execute("UPDATE stato_campionato SET giornata_corrente = 1 WHERE id = 1");
        }

        // 2. Esecuzione
        // Usiamo una lambda vuota per il progressReporter
        campionatoDAO.sincronizzaPunteggiLega(1, msg -> System.out.println(msg));

        // 3. Verifica
        try (Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery("SELECT totale_fantapunti FROM formazioni WHERE id = 1")) {
            if (rs.next()) {
                double totale = rs.getDouble("totale_fantapunti");
                assertTrue("Il punteggio totale dovrebbe essere stato calcolato (> 0)", totale > 0);
            }
        }
    }
}