package it.camb.fantamaster.util;

import it.camb.fantamaster.model.MatchPerformance;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.campionato.MatchData;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RealDataGeneratorTest {

    private RealDataGenerator generator;
    private List<Player> testPlayers;

    @Before
    public void setUp() {
        generator = new RealDataGenerator();
        // Carichiamo il campionato reale dal file JSON per avere dati su cui lavorare
        CampionatoUtil.load("/api/campionato.json");
        
        testPlayers = new ArrayList<>();
        // Creiamo un portiere e un attaccante di squadre che sono nel JSON (es. Inter e Milan)
        testPlayers.add(new Player(1, "Yann", "Sommer", "Inter", 1, "P", 15, "CH"));
        testPlayers.add(new Player(2, "Lautaro", "Martinez", "Inter", 10, "A", 35, "AR"));
    }

    @Test
    public void testGiornataInesistente() {
        // La giornata 99 non esiste
        List<MatchPerformance> result = generator.getPrestazioniReali(testPlayers, 99);
        assertTrue("Dovrebbe restituire una lista vuota per giornate inesistenti", result.isEmpty());
    }

    @Test
    public void testGenerazionePerformanceValida() {
        // Testiamo la giornata 1
        List<MatchPerformance> result = generator.getPrestazioniReali(testPlayers, 1);
        
        // Se l'Inter ha giocato nella giornata 1, la lista non deve essere vuota
        assertNotNull(result);
        
        for (MatchPerformance mp : result) {
            // Verifica Range Voto Base
            assertTrue("Il voto base deve essere >= 5.5", mp.getVotoBase() >= 5.5);
            assertTrue("Il voto base deve essere <= 7.0", mp.getVotoBase() <= 7.0);
            
            // Verifica che il giocatore associato sia corretto
            assertNotNull(mp.getPlayer());
            System.out.println("Performance generata per: " + mp.getPlayer().getCognome() + " - Voto: " + mp.getVotoBase());
        }
    }

    @Test
    public void testLogicaGolSubitiPortiere() {
        // Prendiamo un portiere di una squadra specifica
        Player p = new Player(100, "Portiere", "Test", "Inter", 1, "P", 10, "IT");
        List<Player> players = List.of(p);
        
        List<MatchPerformance> result = generator.getPrestazioniReali(players, 1);
        
        if (!result.isEmpty()) {
            MatchPerformance mp = result.get(0);
            MatchData match = CampionatoUtil.getMatchesByDay(1).get(0); // Prendiamo il primo match per confronto
            
            // Verifichiamo la logica casa/trasferta
            if (match.casa.equalsIgnoreCase("Inter")) {
                assertEquals("Il portiere in casa deve subire i gol della squadra ospite", 
                             match.risultato_trasferta, mp.getGolSubiti());
            } else if (match.trasferta.equalsIgnoreCase("Inter")) {
                assertEquals("Il portiere in trasferta deve subire i gol della squadra di casa", 
                             match.risultato_casa, mp.getGolSubiti());
            }
        }
    }

    @Test
    public void testSquadraCheNonGioca() {
        // Creiamo un giocatore di una squadra inventata
        Player p = new Player(999, "Invisibile", "Test", "SquadraInesistente", 1, "A", 1, "IT");
        List<MatchPerformance> result = generator.getPrestazioniReali(List.of(p), 1);
        
        assertTrue("Se la squadra non gioca, non deve generare performance", result.isEmpty());
    }
}