package it.camb.fantamaster.model;

import java.util.ArrayList;
import java.util.List;

public class Formation {
    private int id;
    private User user;
    private int giornata;
    private String modulo; // Es. "3-4-3"
    private List<FormationPlayer> giocatori = new ArrayList<>();

    // Costruttore, Getters e Setters
    public Formation(User user, int giornata, String modulo) {
        this.user = user;
        this.giornata = giornata;
        this.modulo = modulo;
    }
    
    public List<FormationPlayer> getGiocatori() { return giocatori; }
    public void addGiocatore(FormationPlayer p) { giocatori.add(p); }
    public User getUser() { return user; }
}