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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

//singleton per la gestione delle connessioni al DB
//all'avvio del programma viene creata una sola connessione
public class ConnectionFactory {

    private static ConnectionFactory instance;
    private static Connection connection;
    
    private static final String PROPERTIES_FILE = "database.properties";
    
    // Inizializza le costanti direttamente con i valori letti dal file
    private static final String URL = loadProperty("db.url");
    private static final String USER = loadProperty("db.username");
    private static final String PASSWORD = loadProperty("db.password");


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


    /**
     * Funzione privata per caricare un singolo valore dal file di proprietà.
     * Fallisce (lancia RuntimeException) se il file o la chiave non vengono trovati.
     */
    private static String loadProperty(String key) {
        Properties properties = new Properties();
        try (InputStream inputStream = ConnectionFactory.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            
            if (inputStream == null) {
                throw new RuntimeException("Impossibile trovare il file di configurazione DB: " + PROPERTIES_FILE);
            }
            
            properties.load(inputStream);
            String value = properties.getProperty(key);
            
            if (value == null) {
                throw new RuntimeException("Chiave di configurazione DB mancante: " + key);
            }
            return value;

        } catch (IOException e) {
            throw new RuntimeException("Errore I/O durante il caricamento del file di proprietà.", e);
        }
    }
}
