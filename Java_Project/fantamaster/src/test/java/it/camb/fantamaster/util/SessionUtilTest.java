package it.camb.fantamaster.util;

import it.camb.fantamaster.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class SessionUtilTest {

    private User testUser;
    private final String TEST_EMAIL = "test@junit.com";

    @Before
    public void setUp() {
        // Prepariamo un utente finto per il test
        testUser = new User();
        testUser.setId(999);
        testUser.setUsername("JUnitTester");
        testUser.setEmail(TEST_EMAIL);
    }

    @After
    public void tearDown() {
        // PULIZIA: Importante! Cancelliamo il file di sessione creato durante il test
        // per lasciare l'ambiente pulito.
        SessionUtil.deleteSession(TEST_EMAIL);
    }

    @Test
    public void testCreateAndLoadSession() {
        // 1. Creiamo la sessione (scrive su file)
        SessionUtil.createSession(testUser);

        // 2. Proviamo a ricaricarla (legge da file)
        Session loadedSession = SessionUtil.loadSession(TEST_EMAIL);

        // 3. Verifiche
        assertNotNull("La sessione caricata non deve essere null", loadedSession);
        assertEquals("L'utente nella sessione deve corrispondere a quello salvato", 
                     testUser.getEmail(), loadedSession.getUser().getEmail());
    }

    @Test
    public void testDeleteSession() {
        // Arrange
        SessionUtil.createSession(testUser);
        assertNotNull(SessionUtil.loadSession(TEST_EMAIL)); // Check esistenza pre-delete

        // Act
        boolean deleted = SessionUtil.deleteSession(TEST_EMAIL);

        // Assert
        assertTrue("Il metodo delete deve ritornare true", deleted);
        assertNull("Dopo la cancellazione, loadSession deve ritornare null", 
                   SessionUtil.loadSession(TEST_EMAIL));
    }
    
    @Test
    public void testLoadNonExistentSession() {
        Session session = SessionUtil.loadSession("email_inesistente@fake.com");
        assertNull("Caricare una sessione inesistente deve tornare null senza crashare", session);
    }
}