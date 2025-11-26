/*
 * Questo file contiene credenziali. NON committare nel repository.
 *
 * Per ignorare con git, aggiungi nella radice del repo una riga in .gitignore:
 * src/main/java/it/camb/fantamaster/util/ConnectionFactory.java
 *
 * Se il file è già tracciato, rimuovilo dall'index e committa:
 * git rm --cached src/main/java/it/camb/fantamaster/util/ConnectionFactory.java
 * git commit -m "Remove sensitive ConnectionFactory from index"
 *
 * Consigliato: sposta le credenziali in un file di configurazione non tracciato
 * o usa variabili d'ambiente/properties esterni.
 */

package it.camb.fantamaster.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    // Parametri di configurazione del 
    private static final String URL = "jdbc:mysql://localhost:3306/fantamaster?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "pass123";

    // Metodo statico per ottenere una connessione
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
