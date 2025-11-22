package it.camb.fantamaster.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    // Parametri di configurazione del DB
    private static final String URL = "jdbc:mysql://localhost:3306/fantamaster?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    // Metodo statico per ottenere una connessione
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
