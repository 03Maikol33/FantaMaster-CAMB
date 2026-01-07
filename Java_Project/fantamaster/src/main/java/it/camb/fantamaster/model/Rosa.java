package it.camb.fantamaster.model;

import java.io.Serializable;

public class Rosa implements Serializable {
    private int id;
    private String nomeRosa;
    private int creditiDisponibili;
    // Mettiamo i campi essenziali, gli altri (crediti, punti) li aggiungerà chi gestisce il mercato/calcolo se servono
    
    public Rosa() {}

    public void setCreditiDisponibili(int crediti) {
        this.creditiDisponibili = crediti;
    }

    public int getCreditiDisponibili() {
        return creditiDisponibili;
    }

    public Rosa(int id, String nomeRosa) {
        this.id = id;
        this.nomeRosa = nomeRosa;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomeRosa() { return nomeRosa; }
    public void setNomeRosa(String nomeRosa) { this.nomeRosa = nomeRosa; }
}