package it.camb.fantamaster.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionFactory {

    private static Connection connection; // L'unica istanza condivisa
    
    private static final String PROPERTIES_FILE = "database.properties";
    private static final String URL = loadProperty("db.url");
    private static final String USER = loadProperty("db.username");
    private static final String PASSWORD = loadProperty("db.password");

    // Metodo statico per ottenere LA connessione (Singleton)
    public static Connection getConnection() throws SQLException {
        // Controllo a 3 livelli:
        // 1. connection == null: Mai aperta prima
        // 2. connection.isClosed(): Chiusa esplicitamente
        // 3. !connection.isValid(2): Il server l'ha chiusa per timeout (il check dura max 2 sec)
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            // Se una di queste è vera, ne apriamo una nuova fresca
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        // Altrimenti restituiamo quella vecchia che funziona ancora
        return connection;
    }

    private static String loadProperty(String key) {
        Properties properties = new Properties();
        try (InputStream inputStream = ConnectionFactory.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) throw new RuntimeException("File config non trovato: " + PROPERTIES_FILE);
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException e) {
            throw new RuntimeException("Errore lettura properties", e);
        }
    }
}
// Fix conflitti definitivo