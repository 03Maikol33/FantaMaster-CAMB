package it.camb.fantamaster.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RulesTest {

    private Rules rules;

    @Before
    public void setUp() {
        // Reinizializza l'oggetto prima di ogni test
        this.rules = new Rules();
    }

    @Test
    public void testDefaultBudgetIsCorrect() {
        // Non serve istanziare 'new Rules()', lo ha fatto il @Before
        assertEquals("Il budget iniziale di default deve essere 500", 
                     500, rules.getInitialBudget());
    }

    @Test
    public void testBonusConfiguration() {
        // Verifica che i bonus siano positivi (logica di business)
        assertTrue("Il bonus gol deve essere positivo", rules.getBonusGol() > 0);
        assertTrue("Il bonus assist deve essere positivo", rules.getBonusAssist() > 0);
    }

    @Test
    public void testMalusConfiguration() {
        // Verifica che i malus siano positivi (di solito si sottraggono valori positivi)
        assertTrue("Il malus ammonizione deve essere positivo", rules.getMalusAmmonizione() > 0);
    }
}