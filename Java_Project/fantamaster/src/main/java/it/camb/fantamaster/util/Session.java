package it.camb.fantamaster.util;

import java.io.Serializable;
import java.time.LocalDateTime;

import it.camb.fantamaster.model.User;

public class Session implements Serializable{
    User user;
    LocalDateTime lastAccess;

    /**
     * Ottiene l'utente associato alla sessione.
     * @return L'utente della sessione.
     */
    public User getUser() {
        return user;
    }
    /**
     * Ottiene l'ultima data e ora di accesso della sessione.
     * @return L'ultima data e ora di accesso.
     */
    public LocalDateTime getLastAccess() {
        return lastAccess;
    }

    /**
     * Restituisce una rappresentazione testuale della sessione.
     * @return Una stringa che rappresenta la sessione.
     */
    @Override
    public String toString() {
        return "Session [user=" + user + ", lastAccess=" + lastAccess + "]";
    }
}
