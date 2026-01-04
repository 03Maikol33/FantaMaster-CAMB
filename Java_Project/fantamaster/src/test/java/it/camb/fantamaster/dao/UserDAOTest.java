package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class UserDAOTest {

    private UserDAO userDAO;
    private Connection connection;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        
        // Ricostruiamo la tabella per il test
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "username VARCHAR(255), " +
                         "email VARCHAR(255), " +
                         "hash_password VARCHAR(255), " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "avatar BLOB)");
        }
        userDAO = new UserDAO(connection);
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE utenti");
        }
    }

    @Test
    public void testInsertAndFindById() {
        User user = new User();
        user.setUsername("TestUser");
        user.setEmail("test@email.com");
        user.setHashPassword("hash123");

        boolean inserted = userDAO.insert(user);
        assertTrue(inserted);
        assertNotEquals(0, user.getId()); // ID deve essere stato generato

        User retrieved = userDAO.findById(user.getId());
        assertNotNull(retrieved);
        assertEquals("TestUser", retrieved.getUsername());
    }

    @Test
    public void testUpdateUser() {
        User user = new User();
        user.setUsername("OldName");
        user.setEmail("old@email.com");
        user.setHashPassword("oldHash");
        userDAO.insert(user);

        user.setUsername("NewName");
        boolean updated = userDAO.update(user);
        
        assertTrue(updated);
        User retrieved = userDAO.findById(user.getId());
        assertEquals("NewName", retrieved.getUsername());
    }

    @Test
    public void testDeleteUser() {
        User user = new User();
        user.setUsername("ToDelete");
        user.setEmail("del@email.com");
        user.setHashPassword("pass");
        userDAO.insert(user);

        boolean deleted = userDAO.delete(user.getId());
        assertTrue(deleted);
        assertNull(userDAO.findById(user.getId()));
    }
}