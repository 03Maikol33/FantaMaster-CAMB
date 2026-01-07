package it.camb.fantamaster.model;

import java.io.Serializable;

public class Player implements Serializable {
    private int id;
    private String nome;
    private String cognome;
    private String squadra;
    private int numero;
    private String ruolo;
    private int prezzo;
    private String nazionalita;

    public Player() {
    }

    public Player(int id, String nome, String cognome, String squadra, int numero, String ruolo, int prezzo, String nazionalita) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.squadra = squadra;
        this.numero = numero;
        this.ruolo = ruolo;
        this.prezzo = prezzo;
        this.nazionalita = nazionalita;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return (nome != null ? nome : "") + " " + (cognome != null ? cognome : "");
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getSquadra() {
        return squadra;
    }

    public void setSquadra(String squadra) {
        this.squadra = squadra;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    public int getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(int prezzo) {
        this.prezzo = prezzo;
    }

    public String getNazionalita() {
        return nazionalita;
    }

    public void setNazionalita(String nazionalita) {
        this.nazionalita = nazionalita;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", squadra='" + squadra + '\'' +
                ", numero=" + numero +
                ", ruolo='" + ruolo + '\'' +
                ", prezzo=" + prezzo +
                ", nazionalita='" + nazionalita + '\'' +
                '}';
    }
}
