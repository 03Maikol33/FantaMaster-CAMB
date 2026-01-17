package it.camb.fantamaster.model;

import java.time.LocalDateTime;

import it.camb.fantamaster.util.RequestStatus;

public class Request {
    private int id;
    private League league;
    private User user;
    private LocalDateTime timestamp;
    private RequestStatus status;

    /**
     * Costruttore per una nuova richiesta di accesso alla lega.
     * Lo stato viene impostato automaticamente a "in_attesa".
     *
     * @param league la lega per cui viene richiesto l'accesso
     * @param user l'utente che richiede l'accesso
     */
    public Request( League league, User user) {
        this.league = league;
        this.user = user;
        this.status = RequestStatus.in_attesa;
    }

    /**
     * Costruttore vuoto per Request.
     */
    public Request() {
    }

    /**
     * Costruttore per una richiesta con stato specificato.
     *
     * @param league la lega
     * @param user l'utente
     * @param status lo stato della richiesta
     */
    public Request(League league, User user, RequestStatus status) {
        this.league = league;
        this.user = user;
        this.status = status;
    }

    /**
     * Costruttore per una richiesta con stato booleano (accettata/rifiutata).
     *
     * @param league la lega
     * @param user l'utente
     * @param accepted true se accettata, false se rifiutata
     */
    public Request(League league, User user, Boolean accepted) {
        this.league = league;
        this.user = user;
        this.status = accepted ? RequestStatus.accettata : RequestStatus.rifiutata;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public League getLeague() {
        return league;
    }
    public void setLeague(League league) {
        this.league = league;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public RequestStatus getRequestStatus() {
        return status;
    }
    public void setRequestStatus(RequestStatus status) {
        this.status = status;
    }

    public boolean isAccepted() {
        return this.status == RequestStatus.accettata;
    }
    public void setAccepted(boolean accepted) {
        this.status = accepted ? RequestStatus.accettata : RequestStatus.rifiutata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Rappresentazione in stringa della richiesta.
     */
    @Override
    public String toString(){
        return "Request{" +
                "league=" + league +
                ", user=" + user +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}