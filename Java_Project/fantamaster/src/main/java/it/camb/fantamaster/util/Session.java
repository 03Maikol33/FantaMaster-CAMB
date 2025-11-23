package it.camb.fantamaster.util;

import java.io.Serializable;
import java.time.LocalDateTime;

import it.camb.fantamaster.model.User;

public class Session implements Serializable{
    User user;
    LocalDateTime lastAccess;

    public User getUser() {
        return user;
    }
    public LocalDateTime getLastAccess() {
        return lastAccess;
    }

    @Override
    public String toString() {
        return "Session [user=" + user + ", lastAccess=" + lastAccess + "]";
    }
}
