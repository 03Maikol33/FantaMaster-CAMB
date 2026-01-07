package it.camb.fantamaster.util;

import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.dao.RulesDAO; // 1. Importiamo RulesDAO
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.FantavotoCalculator; // 2. Importiamo il Calcolatore
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.MatchPerformance;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.model.User;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class DataSimulator {

    /**
     * Simula una giornata completa usando le REGOLE REALI della lega.
     */
    public static void simulateDayForUser(User user, League league, int giornata) {
        System.out.println("--- 🎲 INIZIO SIMULAZIONE GIORNATA " + giornata + " per " + user.getUsername() + " ---");
        
        try (Connection conn = ConnectionFactory.getConnection()) {
            UsersLeaguesDAO dao = new UsersLeaguesDAO(conn);
            RosaDAO rosaDAO = new RosaDAO(conn);
            RulesDAO rulesDAO = new RulesDAO(conn); // DAO per le regole
            RealDataGenerator generator = new RealDataGenerator();
            FantavotoCalculator calculator = new FantavotoCalculator(); // Il tuo calcolatore

            // 1. Recupera le regole della lega
            // Se non sono settate nel DB, il DAO restituisce i valori di default
            Rules rules = rulesDAO.getRulesByLeagueId(league.getId());

            // 2. Recupera la Rosa
            int rosaId = dao.getRosaId(user.getId(), league.getId());
            if (rosaId == -1) {
                System.out.println("❌ Nessuna rosa trovata. Fai l'asta prima di simulare!");
                return;
            }

            // 3. Recupera i giocatori
            List<Player> giocatoriRosa = rosaDAO.getPlayersByRosaId(rosaId);
            if (giocatoriRosa.isEmpty()) {
                System.out.println("⚠️ Rosa vuota! Impossibile schierare formazione.");
                return;
            }

            // 4. Mischia e prendi 11 titolari a caso
            Collections.shuffle(giocatoriRosa);
            List<Player> titolari = giocatoriRosa.stream().limit(11).toList();

            // 5. Crea la formazione nel DB
            int formationId = dao.createDummyFormation(rosaId, giornata);

            // 6. Genera voti reali
            CampionatoUtil.load("/api/campionato.json"); 
            List<MatchPerformance> performances = generator.getPrestazioniReali(titolari, giornata);

            if (performances.isEmpty()) {
                System.out.println("⚠️ Nessun voto generato per la giornata " + giornata);
            }

            // 7. Calcola e Salva i voti
            for (MatchPerformance mp : performances) {
                
                // --- USIAMO IL TUO CALCOLATORE UFFICIALE ---
                double fantavoto = calculator.calcola(mp, rules);
                // -------------------------------------------

                dao.savePlayerScore(formationId, mp.getPlayer().getId(), fantavoto, true);
                System.out.println("   -> Voto salvato: " + mp.getPlayer().getCognome() + " = " + String.format("%.1f", fantavoto));
            }

            // 8. Aggiorna Classifica Generale
            dao.updateLeagueRanking(league.getId());
            System.out.println("✅ Simulazione completata (con regole ufficiali)!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}