package it.camb.fantamaster.model;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class TeamScoreCalculatorTest {

    private TeamScoreCalculator calculator;
    private Rules rules;
    private User testUser;
    private List<MatchPerformance> votiGiornata;
    private static final double DELTA = 0.001;

    @Before
    public void setUp() {
        calculator = new TeamScoreCalculator();
        rules = new Rules(); // Regole Default
        testUser = new User();
        testUser.setUsername("Maikol");
        votiGiornata = new ArrayList<>();
    }

    /**
     * Caso 1: Tutti i titolari giocano, nessun bonus capitano.
     */
    @Test
    public void testCalcoloBaseTitolari() {
        Formation f = new Formation(testUser, 1, "1-0-0"); // Formazione minima per test
        Player p1 = new Player(1, "Mario", "Rossi", "Roma", 10, "A", 20, "IT");
        f.addGiocatore(new FormationPlayer(p1, true, 0, false)); // Titolare, non capitano

        votiGiornata.add(new MatchPerformance(p1, 6.0));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        assertEquals(6.0, totale, DELTA);
    }

    /**
     * Caso 2: Il capitano gioca e il suo voto deve raddoppiare.
     */
    @Test
    public void testBonusCapitano() {
        Formation f = new Formation(testUser, 1, "1-0-0");
        Player cap = new Player(1, "Francesco", "Totti", "Roma", 10, "A", 50, "IT");
        f.addGiocatore(new FormationPlayer(cap, true, 0, true)); // Titolare E CAPITANO

        votiGiornata.add(new MatchPerformance(cap, 7.0));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        // Spiegazione: 7.0 * 2 = 14.0
        assertEquals(14.0, totale, DELTA);
    }

    /**
     * Caso 3: Un titolare non gioca (SV) e viene sostituito da un panchinaro dello stesso ruolo.
     */
    @Test
    public void testSostituzioneSuccessoStessoRuolo() {
        Formation f = new Formation(testUser, 1, "1-1-0");
        Player titolare = new Player(1, "Titolare", "Assente", "Roma", 9, "A", 10, "IT");
        Player panchinaro = new Player(2, "Panchina", "Presente", "Roma", 11, "A", 10, "IT");

        f.addGiocatore(new FormationPlayer(titolare, true, 0, false));
        f.addGiocatore(new FormationPlayer(panchinaro, false, 1, false)); // Panchinaro, ordine 1

        // Solo il panchinaro ha voto
        votiGiornata.add(new MatchPerformance(panchinaro, 6.5));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        assertEquals("Deve entrare il panchinaro (6.5)", 6.5, totale, DELTA);
    }

    /**
     * Caso 4: Un titolare è SV ma il panchinaro ha un ruolo diverso (non deve entrare).
     */
    @Test
    public void testSostituzioneFallitaRuoloDiverso() {
        Formation f = new Formation(testUser, 1, "1-1-0");
        Player titolareAtt = new Player(1, "Attaccante", "Assente", "Roma", 9, "A", 10, "IT");
        Player panchinaroDif = new Player(2, "Difensore", "Presente", "Roma", 2, "D", 10, "IT");

        f.addGiocatore(new FormationPlayer(titolareAtt, true, 0, false));
        f.addGiocatore(new FormationPlayer(panchinaroDif, false, 1, false));

        votiGiornata.add(new MatchPerformance(panchinaroDif, 6.0));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        assertEquals("Non può entrare un Difensore per un Attaccante", 0.0, totale, DELTA);
    }

    /**
     * Caso 5: Rispetto dell'ordine di panchina (entra il primo con voto).
     */
    @Test
    public void testOrdinePanchina() {
        Formation f = new Formation(testUser, 1, "1-2-0");
        Player tit = new Player(1, "Tit", "A", "Roma", 9, "A", 10, "IT");
        Player pan1 = new Player(2, "Pan1", "A", "Roma", 10, "A", 10, "IT"); // SV
        Player pan2 = new Player(3, "Pan2", "A", "Roma", 11, "A", 10, "IT"); // 6.0

        f.addGiocatore(new FormationPlayer(tit, true, 0, false));
        f.addGiocatore(new FormationPlayer(pan1, false, 1, false));
        f.addGiocatore(new FormationPlayer(pan2, false, 2, false));

        votiGiornata.add(new MatchPerformance(pan2, 6.0));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        assertEquals("Salta il primo SV ed entra il secondo con voto", 6.0, totale, DELTA);
    }

    /**
     * Caso 6: Limite massimo di 3 sostituzioni.
     */
    @Test
    public void testLimiteSostituzioni() {
        Formation f = new Formation(testUser, 1, "4-4-0");
        // 4 attaccanti titolari tutti SV
        for (int i = 1; i <= 4; i++) {
            Player p = new Player(i, "Tit" + i, "A", "Roma", i, "A", 10, "IT");
            f.addGiocatore(new FormationPlayer(p, true, 0, false));
        }
        // 4 attaccanti in panchina tutti con voto 6.0
        for (int i = 5; i <= 8; i++) {
            Player p = new Player(i, "Pan" + (i-4), "A", "Roma", i, "A", 10, "IT");
            f.addGiocatore(new FormationPlayer(p, false, i-4, false));
            votiGiornata.add(new MatchPerformance(p, 6.0));
        }

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        // Ne devono entrare solo 3: 6.0 * 3 = 18.0
        assertEquals("Massimo 3 sostituzioni ammesse", 18.0, totale, DELTA);
    }

    /**
     * Caso 7: "Panchina Corta"
     * Il titolare è SV, ma in panchina ci sono solo giocatori di ruoli diversi.
     * Risultato atteso: 0.0 punti (nessun cambio possibile).
     */
    @Test
    public void testPanchinaCortaSenzaRuolo() {
        Formation f = new Formation(testUser, 1, "1-0-0");
        Player titolareAtt = new Player(1, "Attaccante", "Tit", "Milan", 9, "A", 10, "IT");
        Player panchinaroDif = new Player(2, "Difensore", "Pan", "Milan", 2, "D", 10, "IT");

        f.addGiocatore(new FormationPlayer(titolareAtt, true, 0, false));
        f.addGiocatore(new FormationPlayer(panchinaroDif, false, 1, false));

        // Il panchinaro ha un ottimo voto, ma è un Difensore e il titolare era un Attaccante
        votiGiornata.add(new MatchPerformance(panchinaroDif, 7.0));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        
        assertEquals("Il punteggio deve essere 0.0 perché non ci sono attaccanti in panchina", 0.0, totale, DELTA);
    }

    /**
     * Caso 8: "Panchina Inutile"
     * Il titolare è SV, il primo panchinaro del ruolo è SV, deve entrare il secondo panchinaro del ruolo.
     * Risultato atteso: Voto del secondo panchinaro.
     */
    @Test
    public void testPanchinaInutileScorrimento() {
        Formation f = new Formation(testUser, 1, "1-2-0");
        Player tit = new Player(1, "Titolare", "A", "Inter", 9, "A", 10, "IT");
        Player pan1 = new Player(2, "Panchina1", "A", "Inter", 10, "A", 10, "IT");
        Player pan2 = new Player(3, "Panchina2", "A", "Inter", 11, "A", 10, "IT");

        f.addGiocatore(new FormationPlayer(tit, true, 0, false));
        f.addGiocatore(new FormationPlayer(pan1, false, 1, false)); // Priorità 1
        f.addGiocatore(new FormationPlayer(pan2, false, 2, false)); // Priorità 2

        // pan1 è SV (non lo mettiamo in votiGiornata o mettiamo 0.0), pan2 prende 6.5
        votiGiornata.add(new MatchPerformance(pan1, 0.0)); 
        votiGiornata.add(new MatchPerformance(pan2, 6.5));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        
        assertEquals("Deve scartare il primo panchinaro SV e prendere il secondo (6.5)", 6.5, totale, DELTA);
    }

    /**
     * Caso 9: "Capitano Sostituito"
     * Il capitano è SV ed esce. Entra una riserva.
     * Risultato atteso: Il voto della riserva NON viene raddoppiato.
     */
    @Test
    public void testCapitanoSostituitoNonRaddoppia() {
        Formation f = new Formation(testUser, 1, "1-1-0");
        Player cap = new Player(1, "Capitano", "A", "Juve", 10, "A", 10, "IT");
        Player ris = new Player(2, "Riserva", "A", "Juve", 11, "A", 10, "IT");

        f.addGiocatore(new FormationPlayer(cap, true, 0, true)); // Titolare e CAPITANO
        f.addGiocatore(new FormationPlayer(ris, false, 1, false)); // Panchinaro

        // Il capitano non gioca, la riserva prende 6.0
        votiGiornata.add(new MatchPerformance(ris, 6.0));

        double totale = calculator.calcolaPunteggioTotale(f, votiGiornata, rules);
        
        // Se il raddoppio fosse applicato alla riserva avremmo 12.0. 
        // Ma la regola (e il tuo codice) raddoppia solo se haPresoVoto(capitano) è vero.
        assertEquals("La riserva entra ma non gode del bonus raddoppio del capitano assente", 6.0, totale, DELTA);
    }
}