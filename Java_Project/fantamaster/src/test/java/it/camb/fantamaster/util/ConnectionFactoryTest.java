package it.camb.fantamaster.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectionFactoryTest {
    
    private Connection conn; // Campo di classe per tenere traccia della connessione

    /**
     * Inizializza la connessione prima di ogni test.
     * @throws SQLException
     */
    @Before // Eseguito prima di OGNI test (Inizializzazione)
    public void setUp() throws SQLException {
        conn = ConnectionFactory.getConnection();
    }
    
    @Test
    public void testConnection() {
        try {
            assertNotNull(conn); // Usa la connessione creata in setUp
            assertFalse(conn.isClosed());
        } catch (SQLException e) {
             fail("Connection should not throw an exception: " + e.getMessage());
        }
    }

    @After // Eseguito dopo OGNI test (Pulizia)
    public void tearDown() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close(); // Chiude la stessa connessione usata nel test
            }
        } catch (SQLException e) {
             // ...
        }
    }
}
