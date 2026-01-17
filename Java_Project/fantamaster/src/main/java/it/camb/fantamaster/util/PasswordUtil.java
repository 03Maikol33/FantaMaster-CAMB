package it.camb.fantamaster.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Genera l'hash della password.
     * @param plainPassword La password in chiaro da hashare.
     * @return L'hash della password.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Verifica la password confrontando quella in chiaro con l'hash memorizzato.
     * @param plainPassword La password in chiaro da verificare.
     * @param hashedPassword L'hash della password memorizzato.
     * @return true se la password corrisponde all'hash, false altrimenti.
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
