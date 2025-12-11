package it.camb.fantamaster.dao;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import it.camb.fantamaster.model.Player;

public class PlayerDAO {
    private static final String LISTONE_JSON_PATH = "/api/listone.json";

    /**
     * Carica tutti i giocatori dal file listone.json
     */
    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        try {
            InputStream inputStream = PlayerDAO.class.getResourceAsStream(LISTONE_JSON_PATH);
            if (inputStream == null) {
                System.err.println("❌ File listone.json non trovato nel classpath!");
                System.err.println("   Percorso cercato: " + LISTONE_JSON_PATH);
                return players;
            }

            // Leggi il contenuto del file
            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();
            
            System.out.println("✅ File listone.json caricato con successo");
            System.out.println("   Dimensione: " + jsonString.length() + " bytes");
            
            // Parsa il JSON
            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
            System.out.println("   Giocatori trovati: " + jsonArray.size());

            for (JsonElement element : jsonArray) {
                Player player = parsePlayerFromJson(element.getAsJsonObject());
                players.add(player);
            }

        } catch (Exception e) {
            System.err.println("❌ Errore durante il caricamento dei giocatori:");
            e.printStackTrace();
        }

        return players;
    }

    /**
     * Restituisce una pagina di giocatori (offset, limit).
     * Questo evita di creare nodi UI per tutti i giocatori alla volta.
     */
    public List<Player> getPlayersPage(int offset, int limit) {
        List<Player> page = new ArrayList<>();
        try {
            InputStream inputStream = PlayerDAO.class.getResourceAsStream(LISTONE_JSON_PATH);
            if (inputStream == null) {
                System.err.println("❌ File listone.json non trovato nel classpath!");
                return page;
            }

            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();

            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
            int total = jsonArray.size();
            if (offset >= total) return page;

            int end = Math.min(offset + limit, total);
            for (int i = offset; i < end; i++) {
                JsonElement element = jsonArray.get(i);
                Player player = parsePlayerFromJson(element.getAsJsonObject());
                page.add(player);
            }

            System.out.println("✅ Caricata pagina giocatori: offset=" + offset + " limit=" + limit + " (caricati=" + page.size() + ")");

        } catch (Exception e) {
            System.err.println("❌ Errore durante il caricamento della pagina dei giocatori:");
            e.printStackTrace();
        }

        return page;
    }

    /**
     * Restituisce il numero totale di giocatori presenti nel file JSON.
     */
    public int getTotalCount() {
        try {
            InputStream inputStream = PlayerDAO.class.getResourceAsStream(LISTONE_JSON_PATH);
            if (inputStream == null) return 0;
            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();
            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
            return jsonArray.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Helper per convertire un JsonObject in un Player
     */
    private Player parsePlayerFromJson(com.google.gson.JsonObject jsonObject) {
        int id = jsonObject.get("id").getAsInt();
        String nome = jsonObject.get("nome").getAsString();
        String cognome = jsonObject.get("cognome").getAsString();
        String squadra = jsonObject.get("squadra").getAsString();
        int numero = jsonObject.get("numero").getAsInt();
        String ruolo = jsonObject.get("ruolo").getAsString();
        int prezzo = jsonObject.get("prezzo").getAsInt();
        String nazionalita = jsonObject.get("nazionalita").getAsString();

        return new Player(id, nome, cognome, squadra, numero, ruolo, prezzo, nazionalita);
    }

    /**
     * Cerca un giocatore per ID
     */
    public Player getPlayerById(int id) {
        List<Player> players = getAllPlayers();
        for (Player player : players) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    /**
     * Cerca giocatori per ruolo
     */
    public List<Player> getPlayersByRole(String ruolo) {
        List<Player> result = new ArrayList<>();
        List<Player> players = getAllPlayers();
        for (Player player : players) {
            if (player.getRuolo().equalsIgnoreCase(ruolo)) {
                result.add(player);
            }
        }
        return result;
    }

    /**
     * Cerca giocatori per squadra
     */
    public List<Player> getPlayersByTeam(String squadra) {
        List<Player> result = new ArrayList<>();
        List<Player> players = getAllPlayers();
        for (Player player : players) {
            if (player.getSquadra().equalsIgnoreCase(squadra)) {
                result.add(player);
            }
        }
        return result;
    }

    /**
     * Restituisce una pagina di giocatori applicando filtri (ruolo e range prezzo).
     * I parametri `minPrice` o `maxPrice` possono essere null per indicare nessun vincolo.
     */
    public List<Player> getPlayersPageFiltered(int offset, int limit, String ruoloFilter, Integer minPrice, Integer maxPrice) {
        List<Player> matches = new ArrayList<>();
        try {
            InputStream inputStream = PlayerDAO.class.getResourceAsStream(LISTONE_JSON_PATH);
            if (inputStream == null) {
                System.err.println("❌ File listone.json non trovato nel classpath!");
                return matches;
            }

            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();

            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();

            // Applico i filtri durante l'iterazione per evitare di creare liste intermedie inutili
            for (JsonElement element : jsonArray) {
                com.google.gson.JsonObject obj = element.getAsJsonObject();
                String ruolo = obj.get("ruolo").getAsString();
                int prezzo = obj.get("prezzo").getAsInt();

                if (ruoloFilter != null && !ruoloFilter.isEmpty() && !ruolo.equalsIgnoreCase(ruoloFilter)) continue;
                if (minPrice != null && prezzo < minPrice) continue;
                if (maxPrice != null && prezzo > maxPrice) continue;

                Player player = parsePlayerFromJson(obj);
                matches.add(player);
            }

            // Pagination on the filtered list
            if (offset >= matches.size()) return new ArrayList<>();
            int end = Math.min(offset + limit, matches.size());
            return new ArrayList<>(matches.subList(offset, end));

        } catch (Exception e) {
            System.err.println("❌ Errore durante il caricamento filtrato dei giocatori:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Restituisce il conteggio totale di giocatori che rispettano i filtri.
     */
    public int getFilteredTotalCount(String ruoloFilter, Integer minPrice, Integer maxPrice) {
        int count = 0;
        try {
            InputStream inputStream = PlayerDAO.class.getResourceAsStream(LISTONE_JSON_PATH);
            if (inputStream == null) return 0;
            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();
            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                com.google.gson.JsonObject obj = element.getAsJsonObject();
                String ruolo = obj.get("ruolo").getAsString();
                int prezzo = obj.get("prezzo").getAsInt();

                if (ruoloFilter != null && !ruoloFilter.isEmpty() && !ruolo.equalsIgnoreCase(ruoloFilter)) continue;
                if (minPrice != null && prezzo < minPrice) continue;
                if (maxPrice != null && prezzo > maxPrice) continue;

                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}
