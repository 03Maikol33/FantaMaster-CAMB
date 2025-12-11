package it.camb.fantamaster.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private int id;
    private String text;
    private LocalDateTime timestamp;
    private User sender; // Chi l'ha mandato
    private int leagueId; // In quale lega

    // Costruttore vuoto
    public Message() {}

    // Costruttore per NUOVO messaggio (da inviare)
    public Message(String text, User sender, int leagueId) {
        this.text = text;
        this.sender = sender;
        this.leagueId = leagueId;
        this.timestamp = LocalDateTime.now();
    }

    // Costruttore COMPLETO (da DB)
    public Message(int id, String text, LocalDateTime timestamp, User sender, int leagueId) {
        this.id = id;
        this.text = text;
        this.timestamp = timestamp;
        this.sender = sender;
        this.leagueId = leagueId;
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }

    // Utility per formattare l'ora nella chat (es. "14:30")
    public String getFormattedTime() {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    @Override
    public String toString() {
        return sender.getUsername() + ": " + text;
    }
}