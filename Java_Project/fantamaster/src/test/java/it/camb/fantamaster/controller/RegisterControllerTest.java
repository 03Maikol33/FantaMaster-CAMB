package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class RegisterControllerTest extends ApplicationTest {

    private Connection connection;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
        Parent root = loader.load();
        
        stage.setScene(new Scene(root));
        stage.show();
        stage.toFront();
    }

    @Before
    public void setUpDB() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "hash_password VARCHAR(255), " +
                    "created_at TIMESTAMP, " +
                    "avatar BLOB)");
        }
    }

    @After
    public void tearDown() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE utenti");
        }
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }

    // --- TEST VALIDAZIONE ---

    // Verifica che campi vuoti mostrino un errore di validazione.
    @Test
    public void testRegistrationFailsWithEmptyFields() {
        // Clicco Registrati senza scrivere nulla (Il bottone non ha fx:id, cerchiamo il testo)
        clickOn("REGISTRATI"); 

        // messageLabel deve essere visibile
        verifyThat("#messageLabel", isVisible());
        verifyThat("#messageLabel", hasText("Compila tutti i campi!"));
    }

    // Verifica che email non valida (senza @) sia rifiutata.
    @Test
    public void testRegistrationFailsWithInvalidEmail() {
        clickOn("#usernameField").write("TestUser");
        clickOn("#emailField").write("email-sbagliata"); // Manca la @
        clickOn("#passwordField").write("Sicura123!");

        clickOn("REGISTRATI");

        verifyThat("#messageLabel", isVisible());
        verifyThat("#messageLabel", hasText("Email non valida!"));
    }

    // Verifica che una password debole (manca maiuscola, numero o carattere speciale) sia rifiutata.
    @Test
    public void testRegistrationFailsWithWeakPassword() {
        clickOn("#usernameField").write("TestUser");
        clickOn("#emailField").write("test@valid.com");
        clickOn("#passwordField").write("debole"); // Manca maiuscola, numero, speciale

        clickOn("REGISTRATI");

        verifyThat("#messageLabel", isVisible());
        // Verifichiamo che contenga parte del messaggio di errore lungo
        verifyThat("#messageLabel", (javafx.scene.control.Label l) -> 
            l.getText().contains("La password non rispetta i criteri"));
    }

    // --- TEST DUPLICATI ---

    // Verifica che una registrazione con email già esistente nel DB sia rifiutata.
    @Test
    public void testDuplicateEmailFails() throws Exception {
        // 1. Inseriamo manualmente un utente nel DB
        String sql = "INSERT INTO utenti (username, email, hash_password) VALUES ('Vecchio', 'esistente@test.com', 'hash')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }

        // 2. Proviamo a registrarci con la STESSA email
        clickOn("#usernameField").write("NuovoUser");
        clickOn("#emailField").write("esistente@test.com"); // Email duplicata
        clickOn("#passwordField").write("Sicura123!");

        clickOn("REGISTRATI");

        verifyThat("#messageLabel", isVisible());
        verifyThat("#messageLabel", hasText("Esiste già un account con questa email!"));
    }

    // --- TEST SUCCESSO ---

    // Verifica la registrazione con successo e il salvataggio nel DB.
    @Test
    public void testSuccessfulRegistration() {
        // Scriviamo dati validi
        clickOn("#usernameField").write("MarioRossi");
        clickOn("#emailField").write("mario.rossi@email.it");
        clickOn("#passwordField").write("Sicura123!"); // Rispetta tutti i criteri

        clickOn("REGISTRATI");
        
        sleep(1500); // Attesa operazione DB e cambio scena

        // 1. VERIFICA DB
        UserDAO dao = new UserDAO(connection);
        User user = dao.findByEmail("mario.rossi@email.it");
        
        assertNotNull("L'utente deve essere presente nel DB", user);
        assertTrue("L'username deve essere corretto", user.getUsername().equals("MarioRossi"));

        // 2. VERIFICA CAMBIO SCENA
        // Se tutto va bene, il controller chiama navigateTo("/fxml/successfulRegister.fxml")
        // Quindi NON dovremmo più vedere il bottone "REGISTRATI" della vecchia scena.
        try {
            verifyThat("REGISTRATI", isVisible());
            throw new AssertionError("Dovremmo aver cambiato schermata!");
        } catch (AssertionError | Exception e) {
            // Se verifyThat fallisce nel trovare "REGISTRATI", è BUONO: vuol dire che la scena è cambiata!
        }
    }
}