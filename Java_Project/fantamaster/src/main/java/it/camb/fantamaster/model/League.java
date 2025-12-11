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
    private LocalDateTime createdAt;
    private String inviteCode;
    
    // NUOVO CAMPO
    private String allowedFormations;

    // Costruttore creazione
    public League(String name, byte[] image, int maxMembers, User creator, LocalDateTime createdAt) {
        this.name = name;
        this.image = image;
        this.maxMembers = maxMembers;
        this.creator = creator;
        this.createdAt = createdAt;
        this.registrationsClosed = false;
        this.participants = new ArrayList<>();
        this.participants.add(creator);
    }

    // Costruttore completo
    public League(int id, String name, byte[] image, int maxMembers, User creator, LocalDateTime createdAt, boolean registrationsClosed, List<User> participants) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.maxMembers = maxMembers;
        this.creator = creator;
        this.createdAt = createdAt;
        this.registrationsClosed = registrationsClosed;
        this.participants = (participants != null) ? participants : new ArrayList<>();
    }

    // --- NUOVI METODI PER I MODULI ---
    public String getAllowedFormations() {
        return allowedFormations;
    }

    public void setAllowedFormations(String allowedFormations) {
        this.allowedFormations = allowedFormations;
    }

    public List<String> getAllowedFormationsList() {
        if (allowedFormations == null || allowedFormations.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(allowedFormations.split(","));
    }
    // ----------------------------------

    public void addParticipant(User user) {
        if (participants.size() < maxMembers) {
            participants.add(user);
        } else {
            throw new IllegalStateException("League is full");
        }
    }

    // Getter e Setter standard
    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }
    public boolean isRegistrationsClosed() { return registrationsClosed; }
    public void setRegistrationsClosed(boolean registrationsClosed) { this.registrationsClosed = registrationsClosed; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    @Override
    public String toString() {
        return "League{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", allowedFormations='" + allowedFormations + '\'' +
                '}';
    }
}