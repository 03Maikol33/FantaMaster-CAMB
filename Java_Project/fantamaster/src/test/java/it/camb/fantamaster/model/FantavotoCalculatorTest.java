package it.camb.fantamaster.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FantavotoCalculatorTest {

    private Rules rules;
    private Player portiere;
    private Player attaccante;
    private static final double DELTA = 0.001; // Tolleranza per i confronti tra double

    @Before
    public void setUp() {
        // Inizializziamo le regole con i valori standard
        rules = new Rules();
        rules.setBonusGol(3.0);
        rules.setBonusAssist(1.0);
        rules.setBonusRigoreParato(3.0);
        rules.setBonusFattoreCampo(2.0);
        rules.setBonusImbattibilita(1.0);
        rules.setMalusGolSubito(1.0);
        rules.setMalusRigoreSbagliato(3.0);
        rules.setMalusAutogol(2.0);
        rules.setMalusAmmonizione(0.5);
        rules.setMalusEspulsione(1.0);

        // Creiamo i giocatori per i vari test
        portiere = new Player(1, "Wojciech", "Szczesny", "Juventus", 1, "P", 15, "Polonia");
        attaccante = new Player(2, "Dusan", "Vlahovic", "Juventus", 9, "A", 35, "Serbia");
    }

    @Test
    public void testVotoBaseSenzaEventi() {
        MatchPerformance perf = new MatchPerformance(attaccante, 6.5);
        double risultato = FantavotoCalculator.calcolaFantavoto(perf, rules);
        assertEquals("Il fantavoto deve corrispondere al voto base se non ci sono eventi", 6.5, risultato, DELTA);
    }

    @Test
    public void testBonusAttacco() {
        MatchPerformance perf = new MatchPerformance(attaccante, 7.0);
        perf.setGolFatti(2); // 2 * 3.0 = +6.0
        perf.setAssist(1);   // 1 * 1.0 = +1.0
        // Totale atteso: 7.0 + 6.0 + 1.0 = 14.0
        double risultato = FantavotoCalculator.calcolaFantavoto(perf, rules);
        assertEquals(14.0, risultato, DELTA);
    }

    @Test
    public void testFattoreCampo() {
        MatchPerformance perf = new MatchPerformance(attaccante, 6.0);
        perf.setFattoreCampo(true); // +2.0
        double risultato = FantavotoCalculator.calcolaFantavoto(perf, rules);
        assertEquals(8.0, risultato, DELTA);
    }

    @Test
    public void testImbattibilitaPortiere() {
        // Caso 1: Portiere con 0 gol subiti -> Riceve bonus
        MatchPerformance perfCleanSheet = new MatchPerformance(portiere, 6.0);
        perfCleanSheet.setGolSubiti(0);
        assertEquals(7.0, FantavotoCalculator.calcolaFantavoto(perfCleanSheet, rules), DELTA);

        // Caso 2: Portiere con 1 gol subito -> Non riceve bonus e prende malus
        MatchPerformance perfConcesso = new MatchPerformance(portiere, 6.0);
        perfConcesso.setGolSubiti(1); // 6.0 - 1.0 = 5.0
        assertEquals(5.0, FantavotoCalculator.calcolaFantavoto(perfConcesso, rules), DELTA);

        // Caso 3: Attaccante con 0 gol subiti -> NON riceve bonus imbattibilità (logica di ruolo)
        MatchPerformance perfAtt = new MatchPerformance(attaccante, 6.0);
        perfAtt.setGolSubiti(0);
        assertEquals(6.0, FantavotoCalculator.calcolaFantavoto(perfAtt, rules), DELTA);
    }

    @Test
    public void testMalusCartelliniERigori() {
        MatchPerformance perf = new MatchPerformance(attaccante, 6.0);
        perf.setAmmonizione(true);      // -0.5
        perf.setEspulsione(true);       // -1.0
        perf.setRigoriSbagliati(1);    // -3.0
        perf.setAutogol(1);            // -2.0
        // Totale atteso: 6.0 - 0.5 - 1.0 - 3.0 - 2.0 = -0.5
        double risultato = FantavotoCalculator.calcolaFantavoto(perf, rules);
        assertEquals(-0.5, risultato, DELTA);
    }

    @Test
    public void testRigoreParatoPortiere() {
        MatchPerformance perf = new MatchPerformance(portiere, 7.0);
        perf.setRigoriParati(1); // +3.0
        double risultato = FantavotoCalculator.calcolaFantavoto(perf, rules);
        // Imbattibilità (+1.0) è implicita perché golSubiti = 0
        assertEquals(11.0, risultato, DELTA);
    }

    @Test
    public void testMetodoWrapperNonStatico() {
        // Testiamo anche il metodo istanza 'calcola' per la coverage totale
        FantavotoCalculator calc = new FantavotoCalculator();
        MatchPerformance perf = new MatchPerformance(attaccante, 6.0);
        assertEquals(6.0, calc.calcola(perf, rules), DELTA);
    }

    @Test
    public void testGiornataPienaPortiere() {
        MatchPerformance perf = new MatchPerformance(portiere, 6.0);
        perf.setRigoriParati(1);   // +3.0
        perf.setGolSubiti(2);      // -2.0 (e nessun bonus imbattibilità)
        perf.setAmmonizione(true); // -0.5
        
        // 6.0 + 3.0 - 2.0 - 0.5 = 6.5
        double risultato = FantavotoCalculator.calcolaFantavoto(perf, rules);
        assertEquals(6.5, risultato, DELTA);
    }
}