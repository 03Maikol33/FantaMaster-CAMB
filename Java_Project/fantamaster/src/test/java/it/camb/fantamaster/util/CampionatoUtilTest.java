package it.camb.fantamaster.util;

import it.camb.fantamaster.model.campionato.GiornataData;
import it.camb.fantamaster.model.campionato.MatchData;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CampionatoUtilTest {

    private static final String JSON_VALIDO = "/api/campionato.json";

    @Before
    public void setUp() {
        // Carichiamo il file reale prima di ogni test per assicurarci che lo stato statico sia pronto
        CampionatoUtil.load(JSON_VALIDO);
    }

    @Test
    public void testLoadSuccess() {
        // Verifichiamo che la lista delle giornate non sia vuota
        List<GiornataData> giornate = CampionatoUtil.getGiornate();
        assertNotNull("La lista giornate non deve essere null", giornate);
        assertFalse("Il file JSON dovrebbe contenere almeno una giornata", giornate.isEmpty());
        System.out.println("Test Load: Trovate " + giornate.size() + " giornate.");
    }

    @Test
    public void testGetAllMatches() {
        List<MatchData> allMatches = CampionatoUtil.getAllMatches();
        assertNotNull(allMatches);
        
        // Calcoliamo manualmente la somma delle partite delle singole giornate
        int sommaPartite = 0;
        for (GiornataData g : CampionatoUtil.getGiornate()) {
            sommaPartite += g.partite.size();
        }
        
        assertEquals("Il totale dei match deve corrispondere alla somma delle partite di ogni giornata", 
                     sommaPartite, allMatches.size());
    }

    @Test
    public void testGetMatchesByDayEsistente() {
        // Testiamo il recupero della Giornata 1 (assumendo che ci sia nel JSON)
        int giornataTarget = 1;
        List<MatchData> matches = CampionatoUtil.getMatchesByDay(giornataTarget);
        
        assertNotNull(matches);
        assertFalse("La giornata 1 dovrebbe avere delle partite", matches.isEmpty());
    }

    @Test
    public void testGetMatchesByDayInesistente() {
        // Cerchiamo una giornata che non esiste (es. la 99)
        List<MatchData> matches = CampionatoUtil.getMatchesByDay(99);
        
        assertNotNull(matches);
        assertTrue("Per una giornata inesistente deve restituire una lista vuota", matches.isEmpty());
    }

    @Test
    public void testGetActionsForMatch() {
        // Prendiamo un match a caso per testare le azioni
        List<MatchData> matches = CampionatoUtil.getAllMatches();
        if (!matches.isEmpty()) {
            MatchData m = matches.get(0);
            assertNotNull("La lista eventi non deve essere null", CampionatoUtil.getActionsForMatch(m));
        }
    }

    @Test
    public void testGetActionsForMatchNull() {
        // Se passiamo un match null, deve restituire una lista vuota (evitando NPE)
        assertTrue("Se il match è null, deve restituire lista vuota", 
                   CampionatoUtil.getActionsForMatch(null).isEmpty());
    }
}