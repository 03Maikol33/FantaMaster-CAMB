package it.camb.fantamaster.util;

public enum RequestStatus {
    in_attesa,
    accettata,
    rifiutata;

    /**
     * Converte una stringa dal database in un valore di RequestStatus.
     * @param status La stringa dallo stato del database.
     * @return Il corrispondente valore di RequestStatus in formato enum.
     */
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

    /**
     * Converte il valore di enum RequestStatus in una stringa per il database.  
     * @return La stringa corrispondente al valore di RequestStatus.
     */
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
