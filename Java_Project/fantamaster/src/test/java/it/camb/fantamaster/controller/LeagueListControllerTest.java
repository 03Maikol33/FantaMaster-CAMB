package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main; // Importa Main
import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.scene.Node; // Import Node
import org.junit.After;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class LeagueListControllerTest extends ApplicationTest {

    private Connection connection;
    private User me;
    private User otherUser;

    @Override
    public void start(Stage stage) throws Exception {
        // 1. INIEZIONE STAGE (Usa il setter che abbiamo creato)
        Main.setPrimaryStage(stage);

        setupDatabase();
        createTestData();
        SessionUtil.createSession(me);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueList.fxml"));
        Parent root = loader.load();
        
        stage.setScene(new Scene(root, 400, 600));
        stage.show();
        stage.toFront();
    }

    private void setupDatabase() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS richieste_accesso (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, stato VARCHAR(50), data_richiesta TIMESTAMP)");
        }
    }

    private void createTestData() {
        UserDAO userDAO = new UserDAO(connection);
        LeagueDAO leagueDAO = new LeagueDAO(connection);
        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(connection);

        // Creo ME (ID autogenerato sarà 1)
        me = new User(); me.setUsername("Me"); me.setEmail("me@test.com"); me.setHashPassword("x");
        userDAO.insert(me);

        // Creo OTHER (ID autogenerato sarà 2)
        otherUser = new User(); otherUser.setUsername("Other"); otherUser.setEmail("other@test.com"); otherUser.setHashPassword("x");
        userDAO.insert(otherUser);

        // Lega Admin (Creatore = ME)
        League myLeague = new League();
        myLeague.setName("Lega Admin");
        myLeague.setMaxMembers(10);
        myLeague.setCreator(me);
        myLeague.setCreatedAt(LocalDateTime.now());
        myLeague.setParticipants(new ArrayList<>());
        leagueDAO.insertLeague(myLeague);

        // Lega Partecipante (Creatore = OTHER)
        League otherLeague = new League();
        otherLeague.setName("Lega Partecipante");
        otherLeague.setMaxMembers(10);
        otherLeague.setCreator(otherUser);
        otherLeague.setCreatedAt(LocalDateTime.now());
        otherLeague.setParticipants(new ArrayList<>());
        leagueDAO.insertLeague(otherLeague); 
        
        ulDAO.subscribeUserToLeague(me, otherLeague);
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
        SessionUtil.deleteSession("me@test.com");
        release(new KeyCode[]{});
        release(new MouseButton[]{});
        FxToolkit.hideStage();
    }

    @Test
    public void testAdminLeagueInteraction() {
        // Verifica esistenza e visibilità
        verifyThat("Lega Admin", isVisible());
        
        // Verifica BADGE (HBox)
        verifyThat("#adminBadgeContainer", isVisible());

        // Clicco
        clickOn("Lega Admin");

        // Verifica navigazione a Admin Screen (bottone Richieste)
        verifyThat("#richiesteButton", isVisible()); 
    }

    @Test
    public void testParticipantLeagueInteraction() {
        verifyThat("Lega Partecipante", isVisible());

        // FIX ASSERTION ERROR:
        // lookup(...) trova TUTTI i nodi con quell'ID, anche quelli con visible=false.
        // Dobbiamo contare solo quelli effettivamente visibili a schermo.
        long visibleBadges = lookup("#adminBadgeContainer").queryAll().stream()
                .filter(Node::isVisible)
                .count();
        
        // Deve essercene 1 solo (quello della Lega Admin), l'altro deve essere nascosto
        assertEquals("Dovrebbe esserci esattamente 1 badge admin visibile", 1, visibleBadges);

        // Clicco
        clickOn("Lega Partecipante");

        // Verifica navigazione a Screen Normale (bottone Impostazioni)
        verifyThat("#impostazioniButton", isVisible());
    }
}