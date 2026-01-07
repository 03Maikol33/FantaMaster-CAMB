package it.camb.fantamaster.model;

import java.time.LocalDateTime;

public class Scambio {
    private int id;
    private int legaId;
    private int rosaRichiedenteId;
    private int rosaRiceventeId;
    private int giocatoreOffertoId;
    private int giocatoreRichiestoId;
    private String stato; // 'proposto', 'accettato', 'rifiutato'
    private LocalDateTime dataProposta;

    // Campi di supporto per la UI (nomi leggibili)
    private String nomeRichiedente;
    private String nomeRicevente;
    private String nomeGiocatoreOfferto;
    private String nomeGiocatoreRichiesto;
    private String ruoloGiocatori; 

    public Scambio() {}

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getLegaId() { return legaId; }
    public void setLegaId(int legaId) { this.legaId = legaId; }
    public int getRosaRichiedenteId() { return rosaRichiedenteId; }
    public void setRosaRichiedenteId(int rosaRichiedenteId) { this.rosaRichiedenteId = rosaRichiedenteId; }
    public int getRosaRiceventeId() { return rosaRiceventeId; }
    public void setRosaRiceventeId(int rosaRiceventeId) { this.rosaRiceventeId = rosaRiceventeId; }
    public int getGiocatoreOffertoId() { return giocatoreOffertoId; }
    public void setGiocatoreOffertoId(int giocatoreOffertoId) { this.giocatoreOffertoId = giocatoreOffertoId; }
    public int getGiocatoreRichiestoId() { return giocatoreRichiestoId; }
    public void setGiocatoreRichiestoId(int giocatoreRichiestoId) { this.giocatoreRichiestoId = giocatoreRichiestoId; }
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    public LocalDateTime getDataProposta() { return dataProposta; }
    public void setDataProposta(LocalDateTime dataProposta) { this.dataProposta = dataProposta; }
    public String getNomeRichiedente() { return nomeRichiedente; }
    public void setNomeRichiedente(String nomeRichiedente) { this.nomeRichiedente = nomeRichiedente; }
    public String getNomeRicevente() { return nomeRicevente; }
    public void setNomeRicevente(String nomeRicevente) { this.nomeRicevente = nomeRicevente; }
    public String getNomeGiocatoreOfferto() { return nomeGiocatoreOfferto; }
    public void setNomeGiocatoreOfferto(String nomeGiocatoreOfferto) { this.nomeGiocatoreOfferto = nomeGiocatoreOfferto; }
    public String getNomeGiocatoreRichiesto() { return nomeGiocatoreRichiesto; }
    public void setNomeGiocatoreRichiesto(String nomeGiocatoreRichiesto) { this.nomeGiocatoreRichiesto = nomeGiocatoreRichiesto; }
    public String getRuoloGiocatori() { return ruoloGiocatori; }
    public void setRuoloGiocatori(String ruoloGiocatori) { this.ruoloGiocatori = ruoloGiocatori; }
}