package it.camb.fantamaster.controller;

import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.PasswordUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class LoginControllerTest extends ApplicationTest {

    private Connection connection;

    // --- 1. AVVIO JAVA FX (Con Pulizia Aggressiva) ---
    @Override
    public void start(Stage stage) throws Exception {
        // AGGRESSIVE CLEANUP: Cancelliamo tutto PRIMA che il controller venga caricato
        deleteSessionFiles();

        // Carichiamo il file FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
        stage.toFront();
    }

    private void deleteSessionFiles() {
        File sessionDir = new File(".sessions");
        if (sessionDir.exists() && sessionDir.isDirectory()) {
            File[] files = sessionDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.delete()) {
                        System.err.println("⚠️ Impossibile cancellare il file sessione: " + f.getName());
                    } else {
                        System.out.println("✅ File sessione rimosso per il test: " + f.getName());
                    }
                }
            }
        }
    }

    // --- 2. CONFIGURAZIONE DATABASE (H2) ---
    @Before
    public void setUp() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            // Tabelle minime
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "hash_password VARCHAR(255), " +
                    "created_at TIMESTAMP, " +
                    "avatar BLOB)");

            // Utente Mario per i test
            String sql = "INSERT INTO utenti (username, email, hash_password) VALUES (?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, "Mario");
                ps.setString(2, "mario@test.com");
                ps.setString(3, PasswordUtil.hashPassword("Password123!"));
                ps.executeUpdate();
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE utenti");
        }
        // Rilascio risorse input robot
        release(new KeyCode[]{});
        release(new MouseButton[]{});
        FxToolkit.hideStage();
    }

    // --- 3. I TEST ---

    // Verifica che con campi email e password vuoti venga mostrato un errore di validazione.
    @Test
    public void testLoginWithEmptyInputShowsError() {
        // Clicca Login vuoto
        clickOn("#loginButton"); 
        // Verifica Errore
        verifyThat("#errorLabel", isVisible());
        // Nota: adatta il testo a quello che hai effettivamente nel controller (es. "Inserisci email...")
        verifyThat("#errorLabel", hasText("Email o Password non valide.")); 
    }

    // Verifica che con password errata venga mostrato l'errore appropriato.
    @Test
    public void testLoginWithWrongPassword() {
        // Login con password sbagliata
        clickOn("#emailField").write("mario@test.com");
        clickOn("#passwordField").write("PasswordSbagliata");
        clickOn("#loginButton");

        verifyThat("#errorLabel", isVisible());
        verifyThat("#errorLabel", hasText("Password errata."));
    }

    // Verifica che il login con un utente inesistente mostri l'errore "Utente non trovato".
    @Test
    public void testLoginWithNonExistentUser() {
        // Login utente inesistente
        clickOn("#emailField").write("fantasma@test.com");
        clickOn("#passwordField").write("qualsiasi");
        clickOn("#loginButton");

        verifyThat("#errorLabel", isVisible());
        verifyThat("#errorLabel", hasText("Utente non trovato. Registrati."));
    }
}