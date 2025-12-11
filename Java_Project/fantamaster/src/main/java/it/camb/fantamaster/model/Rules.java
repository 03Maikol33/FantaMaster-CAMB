package it.camb.fantamaster.model;

public class Rules {
    private int id;
    private int leagueId;
    private int initialBudget;
    
    // Opzioni
    private boolean usaModificatoreDifesa; // Nuovo campo
    
    // Bonus
    private double bonusGol;
    private double bonusAssist;
    private double bonusImbattibilita;
    private double bonusRigoreParato;
    private double bonusFattoreCampo;
    
    // Malus
    private double malusGolSubito;
    private double malusAmmonizione;
    private double malusEspulsione;
    private double malusRigoreSbagliato;
    private double malusAutogol;

    public Rules() {
        // Valori di default (allineati al tuo SQL)
        this.initialBudget = 500;
        this.usaModificatoreDifesa = true;
        
        this.bonusGol = 3.0;
        this.bonusAssist = 1.0;
        this.bonusImbattibilita = 1.0;
        this.bonusRigoreParato = 3.0;
        this.bonusFattoreCampo = 1.0;
        
        this.malusGolSubito = 1.0;
        this.malusAmmonizione = 0.5;
        this.malusEspulsione = 1.0;
        this.malusRigoreSbagliato = 3.0;
        this.malusAutogol = 2.0;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }
    public int getInitialBudget() { return initialBudget; }
    public void setInitialBudget(int initialBudget) { this.initialBudget = initialBudget; }

    public boolean isUsaModificatoreDifesa() { return usaModificatoreDifesa; }
    public void setUsaModificatoreDifesa(boolean usaModificatoreDifesa) { this.usaModificatoreDifesa = usaModificatoreDifesa; }

    public double getBonusGol() { return bonusGol; }
    public void setBonusGol(double bonusGol) { this.bonusGol = bonusGol; }
    public double getBonusAssist() { return bonusAssist; }
    public void setBonusAssist(double bonusAssist) { this.bonusAssist = bonusAssist; }
    public double getBonusImbattibilita() { return bonusImbattibilita; }
    public void setBonusImbattibilita(double bonusImbattibilita) { this.bonusImbattibilita = bonusImbattibilita; }
    public double getBonusRigoreParato() { return bonusRigoreParato; }
    public void setBonusRigoreParato(double bonusRigoreParato) { this.bonusRigoreParato = bonusRigoreParato; }
    public double getBonusFattoreCampo() { return bonusFattoreCampo; }
    public void setBonusFattoreCampo(double bonusFattoreCampo) { this.bonusFattoreCampo = bonusFattoreCampo; }

    public double getMalusGolSubito() { return malusGolSubito; }
    public void setMalusGolSubito(double malusGolSubito) { this.malusGolSubito = malusGolSubito; }
    public double getMalusAmmonizione() { return malusAmmonizione; }
    public void setMalusAmmonizione(double malusAmmonizione) { this.malusAmmonizione = malusAmmonizione; }
    public double getMalusEspulsione() { return malusEspulsione; }
    public void setMalusEspulsione(double malusEspulsione) { this.malusEspulsione = malusEspulsione; }
    public double getMalusRigoreSbagliato() { return malusRigoreSbagliato; }
    public void setMalusRigoreSbagliato(double malusRigoreSbagliato) { this.malusRigoreSbagliato = malusRigoreSbagliato; }
    public double getMalusAutogol() { return malusAutogol; }
    public void setMalusAutogol(double malusAutogol) { this.malusAutogol = malusAutogol; }
}