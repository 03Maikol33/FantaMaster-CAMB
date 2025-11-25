package it.camb.fantamaster.model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.sql.Blob;

public class User implements Serializable {
    private int id;
    private String username;
    private String email;
    private String hashPassword;
    private LocalDateTime createdAt;
    private byte[] avatar;

    // Costruttori
    public User() {}

    public User(int id, String username, String email, String hashPassword, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.hashPassword = hashPassword;
        this.createdAt = createdAt;
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHashPassword() { return hashPassword; }
    public void setHashPassword(String hashPassword) { this.hashPassword = hashPassword; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public byte[] getAvatar() { return avatar; }
    public void setAvatar(byte[] avatar) { this.avatar = avatar; }

    @Override
    public String toString() {
        return "Utente{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof User)) return false;
        User user = (User) obj;
        return (id == user.id) && 
               (username != null ? username.equals(user.username) : user.username == null) &&
               (email != null ? email.equals(user.email) : user.email == null);
    }
}
