package it.camb.fantamaster.util;

public enum RequestStatus {
    in_attesa,
    accettata,
    rifiutata;

    public static RequestStatus fromDb(String status){
        if (status == null) {
            return null;
        }
        switch (status) {
            case "in_attesa":
                return in_attesa;
            case "accettata":
                return accettata;
            case "rifiutata":
                return rifiutata;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    public String toDb() {
        switch (this) {
            case in_attesa:
                return "in_attesa";
            case accettata:
                return "accettata";
            case rifiutata:
                return "rifiutata";
            default:
                throw new IllegalArgumentException("Unknown status: " + this);
        }
    }
}
