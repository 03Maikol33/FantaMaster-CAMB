package it.camb.fantamaster.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionFactory {

    private static Connection connection; 
    
    private static final String PROPERTIES_FILE = "database.properties";
    private static final String URL = loadProperty("db.url");
    private static final String USER = loadProperty("db.username");
    private static final String PASSWORD = loadProperty("db.password");

    // FIX: Forza il caricamento del driver MySQL per evitare l'errore "No suitable driver"
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Errore: Driver MySQL non trovato!");
            ErrorUtil.log("Driver MySQL non trovato", e);
        }
    }

    /**
     * Ottiene una connessione al database.
     * La connessione viene creata una sola volta e riutilizzata;
     * se chiusa, viene ricreata.
     *
     * @return una connessione al database
     * @throws SQLException se si verifica un errore durante la connessione
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Se URL, USER o PASSWORD contengono spazi o sono errati, qui darà errore
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    private static String loadProperty(String key) {
        Properties properties = new Properties();
        try (InputStream inputStream = ConnectionFactory.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) {
                throw new RuntimeException("File di configurazione non trovato: " + PROPERTIES_FILE);
            }
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException e) {
            throw new RuntimeException("Errore durante la lettura del file properties", e);
        }
    }
}