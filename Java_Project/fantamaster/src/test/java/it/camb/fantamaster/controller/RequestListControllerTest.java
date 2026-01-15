package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class RequestListControllerTest extends ApplicationTest {

    private Connection connection;
    private User adminUser;
    private User requesterUser;
    private League testLeague;

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Setup DB e Dati
        setupDatabase();
        createTestData();

        // 2. Simuliamo Login Admin (Best Practice)
        SessionUtil.createSession(adminUser);

        // 3. Caricamento UI Manuale
        // Dobbiamo caricare il loader per ottenere il controller e passargli la lega
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/requestList.fxml"));
        Parent root = loader.load();
        
        // RECUPERO IL CONTROLLER E INIETTO LA LEGA
        RequestListController controller = loader.getController();
        // Questo scatena loadListData() che chiamerà il DB
        controller.setCurrentLeague(testLeague);
        
        stage.setScene(new Scene(root, 400, 600));
        stage.show();
        stage.toFront();
    }

    private void setupDatabase() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            // Creazione schema completo necessario per DAO
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS richieste_accesso (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, stato VARCHAR(50), data_richiesta TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), punteggio_totale DOUBLE DEFAULT 0.0)");
        }
    }

    private void createTestData() {
        UserDAO userDAO = new UserDAO(connection);
        LeagueDAO leagueDAO = new LeagueDAO(connection);
        RequestDAO requestDAO = new RequestDAO(connection);

        // 1. Admin
        adminUser = new User(); 
        adminUser.setUsername("Admin"); 
        adminUser.setEmail("admin@test.com"); 
        adminUser.setHashPassword("x");
        userDAO.insert(adminUser);

        // 2. Utente che vuole entrare
        requesterUser = new User(); 
        requesterUser.setUsername("LuigiRichiedente"); 
        requesterUser.setEmail("luigi@test.com"); 
        requesterUser.setHashPassword("x");
        userDAO.insert(requesterUser);

        // 3. Lega dell'Admin
        testLeague = new League();
        testLeague.setName("Lega Test");
        testLeague.setMaxMembers(10);
        testLeague.setCreator(adminUser);
        testLeague.setCreatedAt(LocalDateTime.now());
        testLeague.setParticipants(new ArrayList<>());
        leagueDAO.insertLeague(testLeague);

        // 4. Richiesta di Iscrizione
        requestDAO.createRequest(requesterUser, testLeague);
    }

    @After
    public void tearDown() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE richieste_accesso");
            stmt.execute("DROP TABLE regole");
            stmt.execute("DROP TABLE utenti_leghe");
            stmt.execute("DROP TABLE leghe");
            stmt.execute("DROP TABLE utenti");
        }
        SessionUtil.deleteSession("admin@test.com");
    }

    // --- I TEST ---

    @Test
    public void testRequestIsVisible() {
        // Attesa caricamento asincrono
        sleep(1500);

        // Verifica: Il nome dell'utente richiedente deve apparire nella lista
        // TestFX cerca una Label con quel testo specifico
        verifyThat("#userName", hasText("LuigiRichiedente"));
        
        // Verifica: I pulsanti di azione devono esserci
        verifyThat("#approveLabel", isVisible());
        verifyThat("#rejectLabel", isVisible());
    }

    @Test
    public void testApproveRequest() {
        sleep(1500);

        // 1. Clicco su APPROVA (l'icona "✔")
        clickOn("#approveLabel");
        
        // Attesa operazione DB e refresh UI
        sleep(1500);

        // 2. VERIFICA DATABASE: L'utente deve essere iscritto
        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(connection);
        boolean isSubscribed = ulDAO.isUserSubscribed(requesterUser, testLeague);
        
        assertTrue("Dopo l'approvazione, l'utente deve essere presente nella tabella utenti_leghe", isSubscribed);
    }
}