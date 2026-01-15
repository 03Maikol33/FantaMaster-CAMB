package it.camb.fantamaster.util;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import it.camb.fantamaster.model.campionato.CampionatoWrapper;
import it.camb.fantamaster.model.campionato.EventoData;
import it.camb.fantamaster.model.campionato.GiornataData;
import it.camb.fantamaster.model.campionato.MatchData;

public class CampionatoUtil {

    private static CampionatoWrapper campionato;

    /**
     * Carica il file JSON dal classpath.
     */
    public static void load(String path) {
        try (InputStreamReader reader = new InputStreamReader(
                CampionatoUtil.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            campionato = new Gson().fromJson(reader, CampionatoWrapper.class);
            if (campionato != null) {
                System.out.println("✅ Campionato caricato: " + campionato.campionato.size() + " giornate trovate.");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore caricamento campionato.json: " + e.getMessage());
            ErrorUtil.log("Errore caricamento campionato.json", e);
        }
    }

    /**
     * Restituisce la lista di tutte le giornate caricate.
     */
    public static List<GiornataData> getGiornate() {
        return (campionato != null) ? campionato.campionato : new ArrayList<>();
    }

    /**
     * Restituisce TUTTE le partite del campionato (di tutte le giornate)
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
     * Restituisce le partite di una specifica giornata
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
     * Restituisce tutte le azioni (eventi) di una specifica partita
     */
    public static List<EventoData> getActionsForMatch(MatchData match) {
        return (match != null) ? match.eventi : new ArrayList<>();
    }
}