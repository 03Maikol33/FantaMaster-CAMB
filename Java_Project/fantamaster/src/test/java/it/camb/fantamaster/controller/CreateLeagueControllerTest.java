package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class CreateLeagueControllerTest extends ApplicationTest {

    private Connection connection;
    private User testUser;

    @Override
    public void start(Stage stage) throws Exception {
        setupDatabase();
        
        // 1. Creiamo l'utente creatore
        UserDAO userDAO = new UserDAO(connection);
        testUser = new User();
        testUser.setUsername("Creator");
        testUser.setEmail("creator@test.com");
        testUser.setHashPassword("pass");
        userDAO.insert(testUser);

        // 2. Login simulato
        SessionUtil.createSession(testUser);

        // 3. Carichiamo la vista
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createLeague.fxml"));
        Parent root = loader.load();
        
        stage.setScene(new Scene(root));
        stage.show();
        stage.toFront();
    }

    private void setupDatabase() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            // CORREZIONE: Aggiornata definizione tabella leghe
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), asta_aperta BOOLEAN DEFAULT FALSE, turno_asta_utente_id INT DEFAULT NULL, giocatore_chiamato_id INT DEFAULT NULL, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
        }
    }

    @After
    public void tearDown() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE regole");
            stmt.execute("DROP TABLE utenti_leghe");
            stmt.execute("DROP TABLE leghe");
            stmt.execute("DROP TABLE utenti");
        }
        SessionUtil.deleteSession("creator@test.com");
        
        release(new KeyCode[]{});
        release(new MouseButton[]{});
        FxToolkit.hideStage();
    }

    // --- TEST 1: Validazione campi vuoti ---
    @Test
    public void testEmptyFieldsError() {
        clickOn("#createLeagueButton");
        
        verifyThat("#messageLabel", isVisible());
        verifyThat("#messageLabel", hasText("Compila tutti i campi e seleziona una modalità."));
    }

    // --- TEST 2: Validazione Numero Pari (Regola di Business) ---
    @Test
    public void testOddParticipantsError() {
        clickOn("#leagueNameField").write("Lega Dispari");
        
        // Seleziono Punti Totali
        clickOn("#gameModeComboBox");
        clickOn("Punti Totali");
        
        // Scrivo un numero dispari (5)
        clickOn("#maxParticipantsField").write("5");
        
        clickOn("#createLeagueButton");

        verifyThat("#messageLabel", isVisible());
        verifyThat("#messageLabel", hasText("Il numero massimo di partecipanti deve essere pari."));
    }

    // --- TEST 3: Blocco Modalità Premium (Scontri Diretti) ---
    @Test
    public void testPremiumModeRejection() {
        // Apro la combo
        clickOn("#gameModeComboBox");
        
        // Clicco su Scontri Diretti -> Questo apre l'Alert!
        clickOn("Scontri Diretti");

        // GESTIONE ALERT
        clickOn("OK"); 

        // Verifico che la selezione sia stata resettata a "Punti Totali"
        ComboBox<String> combo = lookup("#gameModeComboBox").queryComboBox();
        assertEquals("La modalità deve tornare a Punti Totali dopo l'alert", 
                     "Punti Totali", combo.getValue());
    }

    // --- TEST 4: Creazione con Successo ---
    @Test
    public void testSuccessfulCreation() {
        clickOn("#leagueNameField").write("Serie A Test");
        
        clickOn("#gameModeComboBox");
        clickOn("Punti Totali");
        
        // Numero valido e pari
        clickOn("#maxParticipantsField").write("8");
        
        clickOn("#createLeagueButton");

        // Verifica DB
        LeagueDAO dao = new LeagueDAO(connection);
        List<League> leagues = dao.getLeaguesCreatedByUser(testUser);
        
        assertFalse("La lega deve essere stata creata", leagues.isEmpty());
        assertEquals("Serie A Test", leagues.get(0).getName());
        assertEquals(8, leagues.get(0).getMaxMembers());
    }
}