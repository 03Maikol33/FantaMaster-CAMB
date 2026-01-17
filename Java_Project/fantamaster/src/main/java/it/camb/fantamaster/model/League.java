package it.camb.fantamaster.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class League {
    private int id;
    private String name;
    private byte[] image;
    private int maxMembers;
    private User creator;
    private List<User> participants;
    private boolean registrationsClosed;
    private boolean astaAperta;
    private boolean mercatoAperto;
    private LocalDateTime createdAt;
    private String inviteCode;
    private String allowedFormations;
    private int initialBudget;
    private String gameMode;

    //asta
    private Integer turnoAstaUtenteId;   // ID dell'utente a cui tocca (può essere null)
    private Integer giocatoreChiamatoId;

    /**
     * Costruttore vuoto per League.
     */
    public League() {}

    /**
     * Costruttore per la creazione di una nuova lega.
     *
     * @param name il nome della lega
     * @param image l'immagine icona della lega
     * @param maxMembers il numero massimo di partecipanti
     * @param creator l'utente che crea la lega
     * @param gameMode la modalità di gioco (es. punti_totali)
     * @param createdAt la data e ora di creazione
     */
    public League(String name, byte[] image, int maxMembers, User creator, String gameMode, LocalDateTime createdAt) {
        this.name = name;
        this.image = image;
        this.maxMembers = maxMembers;
        this.creator = creator;
        
        if (gameMode == null || gameMode.trim().isEmpty()) {
            this.gameMode = "punti_totali";
        } else {
            this.gameMode = gameMode;
        }

        this.createdAt = createdAt;
        this.registrationsClosed = false;
        this.participants = new ArrayList<>();
        this.participants.add(creator);
        this.initialBudget = 500;
    }

    /**
     * Costruttore completo per League (utilizzato dal DAO per leggere dal database).
     *
     * @param id l'identificativo univoco della lega
     * @param name il nome della lega
     * @param image l'immagine icona della lega
     * @param maxMembers il numero massimo di partecipanti
     * @param creator l'utente che ha creato la lega
     * @param createdAt la data e ora di creazione
     * @param closed true se le iscrizioni sono chiuse
     * @param participants la lista dei partecipanti
     * @param gameMode la modalità di gioco
     * @param astaAperta true se l'asta è aperta
     */
    public League(int id, String name, byte[] image, int maxMembers, User creator, LocalDateTime createdAt, boolean closed, List<User> participants, String gameMode, boolean astaAperta) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.maxMembers = maxMembers;
        this.creator = creator;
        this.createdAt = createdAt;
        this.registrationsClosed = closed;
        this.astaAperta = astaAperta;
        this.participants = (participants != null) ? participants : new ArrayList<>();
        this.initialBudget = 500;
        this.gameMode = gameMode;
    }

    public boolean isAstaAperta() {
        return astaAperta;
    }
    public void setAstaAperta(boolean astaAperta) {
        this.astaAperta = astaAperta;
    }
    public boolean isMercatoAperto() {
        return mercatoAperto;
    }
    public void setMercatoAperto(boolean mercatoAperto) {
        this.mercatoAperto = mercatoAperto;
    }

    public Integer getTurnoAstaUtenteId() {
        return turnoAstaUtenteId;
    }

    public void setTurnoAstaUtenteId(Integer turnoAstaUtenteId) {
        this.turnoAstaUtenteId = turnoAstaUtenteId;
    }

    public Integer getGiocatoreChiamatoId() {
        return giocatoreChiamatoId;
    }

    public void setGiocatoreChiamatoId(Integer giocatoreChiamatoId) {
        this.giocatoreChiamatoId = giocatoreChiamatoId;
    }

    // --- NUOVI METODI PER I MODULI ---
    public String getAllowedFormations() {
        return allowedFormations;
    }

    public boolean isAuctionOpen() {
        // Supponiamo che l'asta sia aperta se le registrazioni non sono chiuse
        return astaAperta;
    }

    public void setAllowedFormations(String allowedFormations) {
        this.allowedFormations = allowedFormations;
    }

    public List<String> getAllowedFormationsList() {
        if (allowedFormations == null || allowedFormations.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(allowedFormations.split("[,;]"));
    }
    // ----------------------------------
    // --- Getter e Setter ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }

    public boolean isRegistrationsClosed() { return registrationsClosed; }
    public void setRegistrationsClosed(boolean registrationsClosed) { this.registrationsClosed = registrationsClosed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public void addParticipant(User user) {
        if (participants == null) participants = new ArrayList<>();
        if (participants.size() < maxMembers) {
            participants.add(user);
        } else {
            throw new IllegalStateException("Lega piena");
        }
    }

    public int getInitialBudget() {
        return initialBudget;
    }
    public void setInitialBudget(int initialBudget) {
        this.initialBudget = initialBudget;
    }

    @Override
    public String toString() {
        return "League{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", allowedFormations='" + allowedFormations + '\'' +
                ", maxMembers=" + maxMembers +
                ", mode='" + gameMode + '\'' +
                '}';
    }
}