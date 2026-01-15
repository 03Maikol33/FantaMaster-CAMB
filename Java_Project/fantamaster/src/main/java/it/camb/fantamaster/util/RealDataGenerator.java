package it.camb.fantamaster.util;

import it.camb.fantamaster.model.MatchPerformance;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.campionato.EventoData;
import it.camb.fantamaster.model.campionato.MatchData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RealDataGenerator {


    private static final Random RANDOM = new Random();
    /**
     * Genera le performance REALI basate sui dati caricati in CampionatoUtil.
     * @param players La lista dei giocatori (es. la rosa o il listone)
     * @param giornata Il numero della giornata da analizzare
     */
    public List<MatchPerformance> getPrestazioniReali(List<Player> players, int giornata) {
        List<MatchPerformance> performanceList = new ArrayList<>();
        
        // 1. Recupera le partite reali di quella giornata
        List<MatchData> partiteGiornata = CampionatoUtil.getMatchesByDay(giornata);

        // Se non ci sono dati per questa giornata, restituisci lista vuota
        if (partiteGiornata == null || partiteGiornata.isEmpty()) {
            System.out.println("⚠️ Nessuna partita trovata per la giornata " + giornata);
            return performanceList;
        }

        // 2. Per ogni giocatore, calcola la performance
        for (Player p : players) {
            // Cerchiamo la partita della squadra del giocatore
            MatchData match = trovaPartitaPerSquadra(partiteGiornata, p.getSquadra());

            if (match != null) {
                // Creiamo la performance. 
                // NOTA: Il JSON non ha il voto base, quindi usiamo un default (es. 6.0) o un random verosimile.
                // Qui uso un random leggero tra 5.5 e 7.0 per variare, ma i BONUS sono reali.
                double votoSimulato = generaVotoBaseSimulato(); 
                MatchPerformance perf = new MatchPerformance(p, votoSimulato);

                // --- A. ANALISI EVENTI (Gol, Assist, Cartellini) ---
                analizzaEventi(match, p, perf);

                // --- B. ANALISI PORTIERI (Gol Subiti) ---
                if ("P".equalsIgnoreCase(p.getRuolo())) {
                    calcolaGolSubiti(match, p, perf);
                }

                performanceList.add(perf);
            }
        }
        return performanceList;
    }

    // Trova la partita in cui ha giocato una certa squadra
    private MatchData trovaPartitaPerSquadra(List<MatchData> partite, String nomeSquadra) {
        for (MatchData m : partite) {
            // Confronto case-insensitive per sicurezza
            if (m.casa.equalsIgnoreCase(nomeSquadra) || m.trasferta.equalsIgnoreCase(nomeSquadra)) {
                return m;
            }
        }
        return null; // Squadra non ha giocato (riposo o rinvio)
    }

    // Popola i bonus/malus leggendo la lista eventi del match
    private void analizzaEventi(MatchData match, Player p, MatchPerformance perf) {
        if (match.eventi == null) return;

        for (EventoData evento : match.eventi) {
            // Controlla se l'evento appartiene al giocatore corrente
            // Usiamo l'ID se corrisponde, altrimenti il nome (più rischioso ma utile se gli ID non sono allineati)
            boolean isPlayer = (evento.id_giocatore == p.getId());
            
            if (isPlayer) {
                String azione = evento.azione.toLowerCase().trim();
                
                // Mappa le stringhe del JSON nei campi di MatchPerformance
                switch (azione) {
                    case "gol":
                        perf.setGolFatti(perf.getGolFatti() + 1);
                        break;
                    case "assist":
                        perf.setAssist(perf.getAssist() + 1);
                        break;
                    case "ammonizione":
                        perf.setAmmonizione(true);
                        break;
                    case "espulsione":
                        perf.setEspulsione(true);
                        break;
                    case "rigore parato":
                        perf.setRigoriParati(perf.getRigoriParati() + 1);
                        break;
                    case "rigore sbagliato":
                        perf.setRigoriSbagliati(perf.getRigoriSbagliati() + 1);
                        break;
                    case "autogol":
                        perf.setAutogol(perf.getAutogol() + 1);
                        break;
                }
            }
        }
    }

    // Calcola i gol subiti guardando il risultato finale
    private void calcolaGolSubiti(MatchData match, Player p, MatchPerformance perf) {
        // Se il giocatore è della squadra di CASA, subisce i gol della TRASFERTA
        if (match.casa.equalsIgnoreCase(p.getSquadra())) {
            perf.setGolSubiti(match.risultato_trasferta);
        } 
        // Se il giocatore è della squadra di TRASFERTA, subisce i gol della CASA
        else if (match.trasferta.equalsIgnoreCase(p.getSquadra())) {
            perf.setGolSubiti(match.risultato_casa);
        }
    }

    private double generaVotoBaseSimulato() {
        // Genera tra 5.5 e 7.0
        return 5.5 + (RANDOM.nextDouble() * 1.5);
    }
}
