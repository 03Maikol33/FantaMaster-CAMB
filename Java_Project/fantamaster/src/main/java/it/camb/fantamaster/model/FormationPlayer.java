package it.camb.fantamaster.model;

public class FormationPlayer {
    private Player player;
    private boolean titolare; // true = Titolare, false = Panchina
    private int ordinePanchina; // 0 per i titolari, 1, 2, 3... per la panchina
    private boolean capitano; // true se è il capitano della formazione

    // Campo per memorizzare il voto finale
    private double fantavotoCalcolato;

    public FormationPlayer(Player player, boolean titolare, int ordinePanchina, boolean capitano) {
        this.player = player;
        this.titolare = titolare;
        this.ordinePanchina = ordinePanchina;
        this.capitano = capitano;
    }

    public Player getPlayer() { return player; }
    
    public boolean isTitolare() { return titolare; }
    
    public int getOrdinePanchina() { return ordinePanchina; }
    
    public boolean isCapitano() { return capitano; }
    public void setCapitano(boolean capitano) { this.capitano = capitano; }

    public double getFantavotoCalcolato() { return fantavotoCalcolato; }
    public void setFantavotoCalcolato(double fantavotoCalcolato) { this.fantavotoCalcolato = fantavotoCalcolato; }
   
    @Override
    public String toString() {
        return String.format("%s %s (Tit:%b, Cap:%b, Voto:%.1f)", 
            player.getNome(), player.getCognome(), titolare, capitano, fantavotoCalcolato);
    }
}