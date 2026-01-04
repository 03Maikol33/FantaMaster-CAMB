package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class RulesDAOTest {

    private RulesDAO rulesDAO;
    private Connection connection;
    private int leagueId = 100; // ID finto per il test

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            // Creiamo solo la tabella regole per questo test unitario
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500, " +
                    "usa_modificatore_difesa BOOLEAN DEFAULT TRUE, " +
                    "bonus_gol DOUBLE DEFAULT 3.0, bonus_assist DOUBLE DEFAULT 1.0, " +
                    "bonus_imbattibilita DOUBLE DEFAULT 1.0, bonus_rigore_parato DOUBLE DEFAULT 3.0, " +
                    "bonus_fattore_campo DOUBLE DEFAULT 1.0, malus_gol_subito DOUBLE DEFAULT 1.0, " +
                    "malus_ammonizione DOUBLE DEFAULT 0.5, malus_espulsione DOUBLE DEFAULT 1.0, " +
                    "malus_rigore_sbagliato DOUBLE DEFAULT 3.0, malus_autogol DOUBLE DEFAULT 2.0)");
        }
        rulesDAO = new RulesDAO(connection);
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE regole");
        }
    }

    @Test
    public void testInsertDefaultAndGet() throws SQLException {
        rulesDAO.insertDefaultRules(leagueId);
        
        Rules rules = rulesDAO.getRulesByLeagueId(leagueId);
        assertNotNull(rules);
        assertEquals(500, rules.getInitialBudget());
        assertEquals(3.0, rules.getBonusGol(), 0.01);
    }

    @Test
    public void testUpdateRules() throws SQLException {
        rulesDAO.insertDefaultRules(leagueId);
        Rules rules = rulesDAO.getRulesByLeagueId(leagueId);
        
        // Modifica
        rules.setInitialBudget(1000);
        rules.setBonusGol(5.0);
        
        boolean updated = rulesDAO.updateRules(leagueId, rules);
        assertTrue(updated);
        
        Rules updatedRules = rulesDAO.getRulesByLeagueId(leagueId);
        assertEquals(1000, updatedRules.getInitialBudget());
        assertEquals(5.0, updatedRules.getBonusGol(), 0.01);
    }
}