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

//singleton per la gestione delle connessioni al DB
//all'avvio del programma viene creata una sola connessione
public class ConnectionFactory {

    private static ConnectionFactory instance;
    private static Connection connection;
    
    // Parametri di configurazione del DB
    private static final String URL = "jdbc:mysql://8lpi42.h.filess.io:3306/fantamaster_toolduckmy?useSSL=true&serverTimezone=UTC";
    private static final String USER = "fantamaster_toolduckmy";
    private static final String PASSWORD = "05f0518209d0fff5d0d1de499a8725ec14ae5ab0";

    public static ConnectionFactory getInstance() {
        if (instance == null) {
            instance = new ConnectionFactory();
        }
        return instance;
    }

    // Metodo statico per ottenere una connessione (sempre la stessa)
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}
