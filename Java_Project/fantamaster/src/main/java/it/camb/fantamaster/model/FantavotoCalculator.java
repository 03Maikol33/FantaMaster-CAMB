package it.camb.fantamaster.model;

import it.camb.fantamaster.model.MatchPerformance;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FantavotoCalculator {

    /**
     * secondo le regole fornite.
     */
    public static double calcolaFantavoto(MatchPerformance performance, Rules rules) {
        double fantavoto = performance.getVotoBase();

        // Bonus
        fantavoto += performance.getGolFatti() * rules.getBonusGol();
        fantavoto += performance.getAssist() * rules.getBonusAssist();
        fantavoto += performance.getRigoriParati() * rules.getBonusRigoreParato();

        if (performance.isFattoreCampo()) {
            fantavoto += rules.getBonusFattoreCampo();
        }

        // Bonus Imbattibilità (Solo per Portieri)
        if (performance.getPlayer().getRuolo().equals("P") && performance.getGolSubiti() == 0) {
            fantavoto += rules.getBonusImbattibilita();
        }

        // Malus
        fantavoto -= performance.getGolSubiti() * rules.getMalusGolSubito();
        fantavoto -= performance.getRigoriSbagliati() * rules.getMalusRigoreSbagliato();
        fantavoto -= performance.getAutogol() * rules.getMalusAutogol();

        if (performance.isAmmonizione()) {
            fantavoto -= rules.getMalusAmmonizione();
        }
        if (performance.isEspulsione()) {
            fantavoto -= rules.getMalusEspulsione();
        }

        return fantavoto;
    }

    /**
     * MOCK: Genera prestazioni casuali per una lista di giocatori.
     * Utile per simulare una giornata di campionato.
     */
    public static List<MatchPerformance> generaPagelleMock(List<Player> players) {
        List<MatchPerformance> pagelle = new ArrayList<>();
        Random random = new Random();

        for (Player p : players) {
            // Genera un voto base realistico tra 4.0 e 8.0
            double votoBase = 4.0 + (8.0 - 4.0) * random.nextDouble();
            // Arrotonda al mezzo punto (es. 6.0, 6.5)
            votoBase = Math.round(votoBase * 2) / 2.0;

            MatchPerformance perf = new MatchPerformance(p, votoBase);

            // Logica casuale in base al ruolo per rendere i dati verosimili
            switch (p.getRuolo()) {
                case "P": // Portiere
                    int tiriInPorta = random.nextInt(6); // 0-5 tiri
                    // Se riceve tiri, può subire gol
                    if (tiriInPorta > 0) {
                        perf.setGolSubiti(random.nextInt(tiriInPorta + 1));
                    }
                    // Possibilità rigore parato (bassa)
                    if (random.nextInt(100) < 5) perf.setRigoriParati(1);
                    break;

                case "D": // Difensore
                    // Più probabile ammonizione
                    if (random.nextInt(100) < 20) perf.setAmmonizione(true);
                    // Raro gol
                    if (random.nextInt(100) < 5) perf.setGolFatti(1);
                    break;

                case "C": // Centrocampista
                    if (random.nextInt(100) < 15) perf.setAmmonizione(true);
                    if (random.nextInt(100) < 10) perf.setAssist(1);
                    if (random.nextInt(100) < 10) perf.setGolFatti(1);
                    break;

                case "A": // Attaccante
                    if (random.nextInt(100) < 30) perf.setGolFatti(1 + random.nextInt(2)); // 1 o 2 gol
                    if (random.nextInt(100) < 10) perf.setAssist(1);
                    if (random.nextInt(100) < 5) perf.setRigoriSbagliati(1);
                    break;
            }

            pagelle.add(perf);
        }
        return pagelle;
    }
}
    

