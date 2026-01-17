package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

public class ProfiloControllerTest extends ApplicationTest {

    private Connection connection;
    private User testUser;

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Otteniamo la connessione dalla Factory (NON chiuderla mai!)
        connection = ConnectionFactory.getConnection();
        
        // 2. Prepariamo il database PRIMA di caricare il controller
        setupDatabase();

        // 3. Prepariamo l'utente e la sessione PRIMA di caricare l'FXML
        // Il controller cerca l'utente in SessionUtil.getCurrentSession() nell'initialize
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("InitialUser");
        testUser.setEmail("profile@test.it");
        testUser.setHashPassword("secret");
        SessionUtil.createSession(testUser);

        // 4. Carichiamo l'FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profilo.fxml"));
        Parent root = loader.load();

        stage.setScene(new Scene(root, 400, 600));
        stage.show();
    }

    private void setupDatabase() throws Exception {
        // Usiamo Statement per configurare H2 senza chiudere la connessione globale
        try (Statement st = connection.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS"); // Pulisce H2 per isolare il test
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            
            // Creazione tabella conforme a UserDAO
            st.execute("CREATE TABLE IF NOT EXISTS utenti (" +
                       "id INT AUTO_INCREMENT PRIMARY KEY, " +
                       "username VARCHAR(255), " +
                       "email VARCHAR(255), " +
                       "hash_password VARCHAR(255), " +
                       "avatar BLOB, " +
                       "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // Inserimento utente di test con ID fisso 1
            st.execute("INSERT INTO utenti (id, username, email, hash_password) " +
                       "VALUES (1, 'InitialUser', 'profile@test.it', 'secret')");
        }
    }

    @After
    public void tearDown() throws Exception {
        // Puliamo la sessione ma NON chiudiamo la connessione della Factory
        SessionUtil.clearSession();
    }

    // Verifica il caricamento iniziale dei dati dell'utente dal DB nel profilo.
    @Test
    public void testInitialLoad() {
        // Attendiamo che l'inizializzazione del controller sia terminata
        sleep(1000);
        
        TextField userField = lookup("#usernameField").queryAs(TextField.class);
        Label emailLabel = lookup("#emailLabel").queryAs(Label.class);

        // Verifichiamo che i dati siano quelli inseriti nel setupDatabase
        assertEquals("Lo username non è stato caricato correttamente", "InitialUser", userField.getText());
        assertEquals("L'email non è stata caricata correttamente", "profile@test.it", emailLabel.getText());
    }

    // Verifica l'aggiornamento dello username e il salvataggio nel DB.
    @Test
    public void testUpdateUsernameSuccess() {
        TextField userField = lookup("#usernameField").queryAs(TextField.class);
        
        // Simula la modifica dello username sulla UI
        clickOn(userField).doubleClickOn(userField).eraseText(15).write("NuovoNome");
        
        // Clicca sul tasto salva (cerca per testo o id se disponibile)
        clickOn("Salva Modifiche");

        // Chiude l'alert di successo (attesa necessaria per il rendering dell'alert)
        sleep(1000);
        clickOn("OK");

        // Verifica la persistenza sul DB usando la stessa connessione
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT username FROM utenti WHERE id = 1")) {
            assertTrue("L'utente con ID 1 dovrebbe esistere", rs.next());
            assertEquals("Lo username sul DB non è stato aggiornato", "NuovoNome", rs.getString("username"));
        } catch (Exception e) {
            fail("Errore durante la verifica sul database: " + e.getMessage());
        }
    }

    // Verifica la validazione: username vuoto non viene salvato nel DB.
    @Test
    public void testEmptyUsernameValidation() {
        TextField userField = lookup("#usernameField").queryAs(TextField.class);
        
        // Svuota il campo
        clickOn(userField).doubleClickOn(userField).eraseText(20);
        
        clickOn("Salva Modifiche");

        // Chiude l'alert di warning che avvisa dello username vuoto
        sleep(1000);
        clickOn("OK");
        
        // Verifica che nel DB il valore non sia cambiato
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT username FROM utenti WHERE id = 1")) {
            rs.next();
            assertEquals("Lo username nel DB non deve cambiare se l'input è vuoto", "InitialUser", rs.getString("username"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    // Verifica la gestione degli errori di database durante il salvataggio.
    @Test
    public void testDatabaseErrorDuringSave() throws Exception {
        // Simula un errore di database cancellando la tabella invece di chiudere la connessione
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE utenti");
        }

        clickOn("#usernameField").write("ErrorMode");
        clickOn("Salva Modifiche");

        // Dovrebbe apparire l'alert di errore di connessione
        sleep(1000);
        clickOn("OK");
        
        // Ripristiniamo la tabella per non corrompere gli altri test della suite
        setupDatabase();
    }
}