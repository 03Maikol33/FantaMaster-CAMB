package it.camb.fantamaster.model;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamScoreCalculator {

    private static final int MAX_SOSTITUZIONI = 3; // Numero massimo di cambi ammessi (solitamente 3 o 5)

    /**
     * Calcola il punteggio totale della squadra applicando le sostituzioni e il bonus capitano.
     * * @param formazione La formazione schierata dall'utente.
     * @param votiGiornata La lista di TUTTE le performance (voti reali) della giornata.
     * @param rules Le regole della lega (per i bonus/malus).
     * @return La somma totale dei fantavoti della squadra.
     */
    public double calcolaPunteggioTotale(Formation formazione, List<MatchPerformance> votiGiornata, Rules rules) {
        double totaleSquadra = 0.0;
        int sostituzioniFatte = 0;
        FantavotoCalculator calculator = new FantavotoCalculator(); // Usiamo il tuo calcolatore singolo

        // 1. Mappa veloce: ID Giocatore -> MatchPerformance (per trovare subito il voto)
        Map<Integer, MatchPerformance> mappaVoti = votiGiornata.stream()
            .collect(Collectors.toMap(mp -> mp.getPlayer().getId(), mp -> mp));

        // 2. Separiamo Titolari e Panchinari (ordinati per priorità)
        List<FormationPlayer> titolari = formazione.getGiocatori().stream()
            .filter(FormationPlayer::isTitolare)
            .collect(Collectors.toList());

        List<FormationPlayer> panchina = formazione.getGiocatori().stream()
            .filter(fp -> !fp.isTitolare())
            .sorted(Comparator.comparingInt(FormationPlayer::getOrdinePanchina)) 
            .collect(Collectors.toList());

        System.out.println("--- INIZIO CALCOLO SQUADRA: " + formazione.getUser().getUsername() + " ---");

        // 3. Ciclo sui TITOLARI
        for (FormationPlayer fpTitolare : titolari) {
            MatchPerformance perf = mappaVoti.get(fpTitolare.getPlayer().getId());
            double votoEffettivo = 0.0;

            // CASO A: Il Titolare ha giocato (ha preso voto)
            if (haPresoVoto(perf)) {
                votoEffettivo = calculator.calcola(perf, rules);
                
                // --- LOGICA CAPITANO ---
                if (fpTitolare.isCapitano()) {
                    System.out.println("©️ CAPITANO: " + fpTitolare.getPlayer().getCognome() + " raddoppia! (" + votoEffettivo + " -> " + (votoEffettivo * 2) + ")");
                    votoEffettivo = votoEffettivo * 2;
                } else {
                    System.out.println("✅ Gioca: " + fpTitolare.getPlayer().getCognome() + " -> " + votoEffettivo);
                }

                fpTitolare.setFantavotoCalcolato(votoEffettivo);
                totaleSquadra += votoEffettivo;
            } 
            // CASO B: Il Titolare NON ha giocato (SV) -> Tentiamo il cambio
            else {
                System.out.print("❌ SV: " + fpTitolare.getPlayer().getCognome());
                boolean sostituito = false;

                if (sostituzioniFatte < MAX_SOSTITUZIONI) {
                    // Cerchiamo un panchinaro dello STESSO RUOLO
                    for (FormationPlayer fpPanchinaro : panchina) {
                        
                        // Criteri: Stesso ruolo, non ancora entrato (votoCalcolato == 0), e ha giocato realmente
                        boolean stessoRuolo = fpPanchinaro.getPlayer().getRuolo().equals(fpTitolare.getPlayer().getRuolo());
                        MatchPerformance perfPanchina = mappaVoti.get(fpPanchinaro.getPlayer().getId());
                        
                        if (stessoRuolo && fpPanchinaro.getFantavotoCalcolato() == 0.0 && haPresoVoto(perfPanchina)) {
                            
                            // Trovato sostituto!
                            votoEffettivo = calculator.calcola(perfPanchina, rules);
                            
                            // Nota: Se entra un panchinaro al posto del capitano, solitamente NON prende il doppio,
                            // a meno che tu non abbia implementato il "Vice Capitano". Qui prende voto normale.
                            
                            fpPanchinaro.setFantavotoCalcolato(votoEffettivo); // Segniamo che è entrato
                            totaleSquadra += votoEffettivo;
                            
                            sostituzioniFatte++;
                            sostituito = true;
                            System.out.println(" -> 🔄 ENTRA " + fpPanchinaro.getPlayer().getCognome() + " (" + votoEffettivo + ")");
                            break; // Esco dal ciclo panchina, passo al prossimo titolare
                        }
                    }
                }

                if (!sostituito) {
                    System.out.println(" -> ⛔ Nessun cambio disponibile (0 punti)");
                    fpTitolare.setFantavotoCalcolato(0.0);
                }
            }
        }

        System.out.println("🏆 TOTALE SQUADRA: " + totaleSquadra);
        return totaleSquadra;
    }

    /**
     * Verifica se una performance contiene un voto valido (ha giocato).
     */
    private boolean haPresoVoto(MatchPerformance mp) {
        // Se l'oggetto è null o il voto base è 0 (o SV), non ha giocato.
        // Nota: Assumiamo che "SV" sia rappresentato da votoBase 0.0 o null
        return mp != null && mp.getVotoBase() > 0.0;
    }
}