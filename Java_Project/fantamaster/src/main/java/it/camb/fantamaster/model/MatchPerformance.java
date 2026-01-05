package it.camb.fantamaster.model;

public class MatchPerformance {
    private Player player;
    private double votoBase; // Il voto del giornalista
    private int golFatti;
    private int golSubiti;
    private int assist;
    private int rigoriParati;
    private boolean fattoreCampo;
    private int rigoriSbagliati;
    private int autogol;
    private boolean ammonizione;
    private boolean espulsione;

    public MatchPerformance(Player player, double votoBase) {
        this.player = player;
        this.votoBase = votoBase;
    }

    // Getters e Setters per tutti i campi per permettere la modifica
    public Player getPlayer() { return player; }
    public double getVotoBase() { return votoBase; }
    public void setVotoBase(double votoBase) { this.votoBase = votoBase; }

    public int getGolFatti() { return golFatti; }
    public void setGolFatti(int golFatti) { this.golFatti = golFatti; }

    public int getGolSubiti() { return golSubiti; }
    public void setGolSubiti(int golSubiti) { this.golSubiti = golSubiti; }

    public int getAssist() { return assist; }
    public void setAssist(int assist) { this.assist = assist; }

    public int getRigoriParati() { return rigoriParati; }
    public void setRigoriParati(int rigoriParati) { this.rigoriParati = rigoriParati; }

    public boolean isFattoreCampo() { return fattoreCampo; }
    public void setFattoreCampo(boolean fattoreCampo) { this.fattoreCampo = fattoreCampo; }

    public int getRigoriSbagliati() { return rigoriSbagliati; }
    public void setRigoriSbagliati(int rigoriSbagliati) { this.rigoriSbagliati = rigoriSbagliati; }

    public int getAutogol() { return autogol; }
    public void setAutogol(int autogol) { this.autogol = autogol; }

    public boolean isAmmonizione() { return ammonizione; }
    public void setAmmonizione(boolean ammonizione) { this.ammonizione = ammonizione; }

    public boolean isEspulsione() { return espulsione; }
    public void setEspulsione(boolean espulsione) { this.espulsione = espulsione; }

    @Override
    public String toString() {
        return String.format("%s %s (Voto: %.1f, Bonus/Malus vari...)", 
            player.getNome(), player.getCognome(), votoBase);
    }
}