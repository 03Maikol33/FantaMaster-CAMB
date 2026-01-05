package it.camb.fantamaster.util;

import com.google.gson.Gson;
import it.camb.fantamaster.model.campionato.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CampionatoUtil {

    private static CampionatoWrapper campionato;

    /**
     * Carica il file JSON dal classpath.
     */
    public static void load(String path) {
        try (InputStreamReader reader = new InputStreamReader(
                CampionatoUtil.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            campionato = new Gson().fromJson(reader, CampionatoWrapper.class);
            System.out.println("✅ Campionato caricato: " + campionato.campionato.size() + " giornate trovate.");
        } catch (Exception e) {
            System.err.println("❌ Errore caricamento campionato.json");
            e.printStackTrace();
        }
    }

    /**
     * Metodo 1: Restituisce TUTTE le partite del campionato (di tutte le giornate)
     */
    public static List<MatchData> getAllMatches() {
        List<MatchData> allMatches = new ArrayList<>();
        if (campionato != null) {
            for (GiornataData g : campionato.campionato) {
                allMatches.addAll(g.partite);
            }
        }
        return allMatches;
    }

    /**
     * Metodo Helper: Restituisce le partite di una specifica giornata
     */
    public static List<MatchData> getMatchesByDay(int giornata) {
        if (campionato != null) {
            for (GiornataData g : campionato.campionato) {
                if (g.giornata == giornata) return g.partite;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Metodo 2: Restituisce tutte le azioni (eventi) di una specifica partita
     */
    public static List<EventoData> getActionsForMatch(MatchData match) {
        return match != null ? match.eventi : new ArrayList<>();
    }
}