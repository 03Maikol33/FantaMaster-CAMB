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

import static org.junit.Assert.*;

public class LeagueAuctionDAOTest {

    private LeagueDAO leagueDAO;
    private UserDAO userDAO;
    private Connection connection;
    private User admin;
    private User participant;
    private League league;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        
        // 1. PULIZIA E CREAZIONE SCHEMA COMPLETO
        initDatabase();

        userDAO = new UserDAO(connection);
        leagueDAO = new LeagueDAO(connection);

        // 2. CREAZIONE UTENTI
        admin = new User(); 
        admin.setUsername("Admin"); 
        admin.setEmail("admin@test.it"); 
        admin.setHashPassword("secret");
        userDAO.insert(admin);
        
        participant = new User(); 
        participant.setUsername("User1"); 
        participant.setEmail("user1@test.it"); 
        participant.setHashPassword("secret");
        userDAO.insert(participant);

        // 3. CREAZIONE LEGA (Con asta aperta)
        // Usiamo il costruttore completo per assicurarci che i flag siano corretti
        league = new League(0, "AstaLeague", null, 4, admin, LocalDateTime.now(), true, new ArrayList<>(), "punti_totali", true);
        leagueDAO.insertLeague(league);
        // league.getId() ora è popolato grazie al recupero delle chiavi generate nel DAO
    }

    private void initDatabase() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // Disabilitiamo i vincoli e cancelliamo tutto per evitare conflitti con altri test
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // TABELLA UTENTI
            st.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255) UNIQUE, hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            
            // TABELLA LEGHE (v5.5 COMPLETA)
            st.execute("CREATE TABLE leghe (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, " +
                "iscrizioni_chiuse BOOLEAN DEFAULT FALSE, created_at TIMESTAMP, codice_invito VARCHAR(255), " +
                "modalita VARCHAR(50), moduli_consentiti VARCHAR(255), mercato_aperto BOOLEAN DEFAULT FALSE, " +
                "asta_aperta BOOLEAN DEFAULT TRUE, turno_asta_utente_id INT, giocatore_chiamato_id INT)");

            // TABELLA UTENTI_LEGHE
            st.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            
            // TABELLA REGOLE (Deve avere tutte le colonne per evitare che l'insertDefaultRules del DAO fallisca)
            st.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500, " +
                "max_portieri INT DEFAULT 3, max_difensori INT DEFAULT 8, max_centrocampisti INT DEFAULT 8, max_attaccanti INT DEFAULT 6, " +
                "voto_base DECIMAL(4,1) DEFAULT 6.0, bonus_gol DECIMAL(4,1) DEFAULT 3.0, bonus_assist DECIMAL(4,1) DEFAULT 1.0, " +
                "bonus_imbattibilita DECIMAL(4,1) DEFAULT 1.0, bonus_rigore_parato DECIMAL(4,1) DEFAULT 3.0, " +
                "bonus_fattore_campo DECIMAL(4,1) DEFAULT 1.0, malus_gol_subito DECIMAL(4,1) DEFAULT 1.0, " +
                "malus_ammonizione DECIMAL(4,1) DEFAULT 0.5, malus_espulsione DECIMAL(4,1) DEFAULT 1.0, " +
                "malus_rigore_sbagliato DECIMAL(4,1) DEFAULT 3.0, malus_autogol DECIMAL(4,1) DEFAULT 2.0)");
            
            // TABELLA ROSA
            st.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), " +
                "crediti_residui INT DEFAULT 500, punteggio_totale DOUBLE DEFAULT 0.0)");
        }
    }

    @Test
    public void testAstaStatusReading() {
        // Recuperiamo la lega dal DB per vedere se i flag sono stati salvati correttamente
        League l = leagueDAO.getLeagueById(league.getId());
        
        assertNotNull("La lega non deve essere null. Se è null, l'insert è fallita!", l);
        assertTrue("L'asta dovrebbe risultare aperta nel DB", l.isAuctionOpen());
    }

    @Test
    public void testUpdateTurnoAsta() {
        // Verifica pre-condizione
        League l = leagueDAO.getLeagueById(league.getId());
        assertNotNull(l);
        assertNull("All'inizio il turno deve essere null", l.getTurnoAstaUtenteId());

        // AZIONE: Assegno il turno al partecipante
        boolean updated = leagueDAO.updateTurnoAsta(league.getId(), participant.getId(), null);
        assertTrue("L'aggiornamento del turno deve restituire true", updated);

        // VERIFICA POST-CONDIZIONE
        League updatedLeague = leagueDAO.getLeagueById(league.getId());
        assertEquals("Il turno deve essere stato assegnato correttamente a User1", 
                     (Integer) participant.getId(), updatedLeague.getTurnoAstaUtenteId());
    }
}