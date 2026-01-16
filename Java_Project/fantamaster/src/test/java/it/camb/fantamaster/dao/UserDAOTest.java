package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class UserDAOTest {

    private UserDAO userDAO;
    private Connection connection;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionFactory.getConnection();
        // Usiamo la pulizia totale per evitare conflitti
        initDatabase();
        userDAO = new UserDAO(connection);
    }

    private void initDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            stmt.execute("CREATE TABLE utenti (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "username VARCHAR(255) UNIQUE, " + // Aggiunto UNIQUE per testare errori
                         "email VARCHAR(255) UNIQUE, " +    // Aggiunto UNIQUE
                         "hash_password VARCHAR(255), " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "avatar BLOB)");
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

    @Test
    public void testInsertSuccessAndFindMethods() {
        User user = new User();
        user.setUsername("Maikol");
        user.setEmail("maikol@test.it");
        user.setHashPassword("secret");

        // Test INSERT e generatedKeys
        assertTrue(userDAO.insert(user));
        assertTrue(user.getId() > 0);

        // Test findByEmail
        assertNotNull(userDAO.findByEmail("maikol@test.it"));
        assertNull(userDAO.findByEmail("non-esisto@test.it"));

        // Test findByUsername
        assertNotNull(userDAO.findByUsername("Maikol"));
        assertNull(userDAO.findByUsername("Sconosciuto"));
    }

    @Test
    public void testFindAll() {
        User u1 = new User(); u1.setUsername("U1"); u1.setEmail("e1@t.it"); u1.setHashPassword("p");
        User u2 = new User(); u2.setUsername("U2"); u2.setEmail("e2@t.it"); u2.setHashPassword("p");
        userDAO.insert(u1);
        userDAO.insert(u2);

        List<User> users = userDAO.findAll();
        assertEquals(2, users.size());
    }

    @Test
    public void testAvatarAndEmailOperations() {
        User user = new User();
        user.setUsername("AvatarUser");
        user.setEmail("avatar@test.it");
        userDAO.insert(user);

        // Test updateAvatar
        byte[] fakeAvatar = new byte[]{1, 2, 3};
        assertTrue(userDAO.updateAvatar(user.getId(), fakeAvatar));

        // Test getAvatarById
        byte[] retrieved = userDAO.getAvatarById(user.getId());
        assertArrayEquals(fakeAvatar, retrieved);
        assertNull(userDAO.getAvatarById(-999)); // Caso non trovato

        // Test getEmailById
        assertEquals("avatar@test.it", userDAO.getEmailById(user.getId()));
        assertNull(userDAO.getEmailById(-999));
    }

    @Test
    public void testUpdateUsername() {
        User user = new User();
        user.setUsername("OldName");
        user.setEmail("update@test.it");
        userDAO.insert(user);

        assertTrue(userDAO.updateUsername(user.getId(), "NewName"));
        User updated = userDAO.findById(user.getId());
        assertEquals("NewName", updated.getUsername());
    }

    // --- TEST PER COPRIRE GLI ERRORI (Blocchi Catch e SQLException) ---

    @Test
    public void testInsertDuplicateEmail() {
        User u1 = new User(); u1.setUsername("U1"); u1.setEmail("dup@test.it");
        userDAO.insert(u1);

        User u2 = new User(); u2.setUsername("U2"); u2.setEmail("dup@test.it"); // Email duplicata
        // Questo attiverà il catch (SQLException e) in insert()
        assertFalse(userDAO.insert(u2));
    }

    @Test
    public void testSqlExceptionsByClosingConnection() throws SQLException {
        // Chiudiamo la connessione volutamente per forzare l'errore in tutti i metodi
        connection.close();

        // Ora tutti i metodi entreranno nel blocco catch (SQLException e)
        // e ErrorUtil.log verrà chiamato, coprendo le righe rosse su Sonar
        assertNull(userDAO.findById(1));
        assertNull(userDAO.findByEmail("x"));
        assertNull(userDAO.findByUsername("x"));
        assertTrue(userDAO.findAll().isEmpty());
        assertFalse(userDAO.update(new User()));
        assertFalse(userDAO.delete(1));
        assertFalse(userDAO.updateUsername(1, "x"));
        assertFalse(userDAO.updateAvatar(1, null));
        assertNull(userDAO.getAvatarById(1));
        assertNull(userDAO.getEmailById(1));
    }
}