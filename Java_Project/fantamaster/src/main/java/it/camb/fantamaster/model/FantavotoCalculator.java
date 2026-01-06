package it.camb.fantamaster.model;

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

public double calcola(MatchPerformance performance, Rules rules){
    return calcolaFantavoto(performance, rules);
}
}
    

