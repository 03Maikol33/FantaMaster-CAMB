package it.camb.fantamaster.model;

import java.time.LocalDateTime;

import it.camb.fantamaster.util.RequestStatus;

public class Request {
    private League league;
    private User user;
    private LocalDateTime timestamp;
    private RequestStatus status;

    public Request( League league, User user) {
        this.league = league;
        this.user = user;
        this.status = RequestStatus.in_attesa;
    }

    public Request() {
    }

    public Request(League league, User user, RequestStatus status) {
        this.league = league;
        this.user = user;
        this.status = status;
    }

    public Request(League league, User user, Boolean accepted) {
        this.league = league;
        this.user = user;
        this.status = accepted ? RequestStatus.accettata : RequestStatus.rifiutata;
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

    //to string
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
