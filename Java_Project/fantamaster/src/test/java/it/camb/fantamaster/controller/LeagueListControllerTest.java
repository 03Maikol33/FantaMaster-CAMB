package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main;
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
import javafx.scene.Node; 
import org.junit.After;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class LeagueListControllerTest extends ApplicationTest {

    private Connection connection;
    private User me;
    private User otherUser;

    @Override
    public void start(Stage stage) throws Exception {
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
            // 1. PULIZIA TOTALE: Risolve l'errore "expected 1 but was 3" (badge duplicati)
            stmt.execute("DROP ALL OBJECTS");

            stmt.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            
            stmt.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN DEFAULT FALSE, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), mercato_aperto BOOLEAN DEFAULT FALSE, asta_aperta BOOLEAN DEFAULT TRUE, turno_asta_utente_id INT DEFAULT NULL, giocatore_chiamato_id INT DEFAULT NULL, budget_iniziale INT DEFAULT 500)");
            
            stmt.execute("CREATE TABLE utenti_leghe (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT)");

            // 2. TABELLE MANCANTI: Risolvono i crash dei DAO durante il test
            stmt.execute("CREATE TABLE scambi (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, rosa_richiedente_id INT, rosa_ricevente_id INT, giocatore_offerto_id INT, giocatore_richiesto_id INT, stato VARCHAR(50))");
            stmt.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE rosa (id INT AUTO_INCREMENT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), crediti_residui INT DEFAULT 500, punteggio_totale DOUBLE DEFAULT 0.0)");
            stmt.execute("CREATE TABLE stato_campionato (id INT PRIMARY KEY, giornata_corrente INT)");
            stmt.execute("INSERT INTO stato_campionato (id, giornata_corrente) VALUES (1, 0)");
            stmt.execute("CREATE TABLE richieste_accesso (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, stato VARCHAR(50), data_richiesta TIMESTAMP)");
        }
    }

    private void createTestData() throws Exception {
        UserDAO userDAO = new UserDAO(connection);
        LeagueDAO leagueDAO = new LeagueDAO(connection);
        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(connection);

        me = new User(); me.setUsername("Me"); me.setEmail("me@test.com"); me.setHashPassword("x");
        userDAO.insert(me);

        otherUser = new User(); otherUser.setUsername("Other"); otherUser.setEmail("other@test.com"); otherUser.setHashPassword("x");
        userDAO.insert(otherUser);

        League myLeague = new League();
        myLeague.setName("Lega Admin");
        myLeague.setMaxMembers(10);
        myLeague.setCreator(me);
        myLeague.setCreatedAt(LocalDateTime.now());
        myLeague.setParticipants(new ArrayList<>());
        leagueDAO.insertLeague(myLeague);

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
            stmt.execute("DROP ALL OBJECTS");
        }
        SessionUtil.deleteSession("me@test.com");
        release(new KeyCode[]{});
        release(new MouseButton[]{});
        FxToolkit.hideStage();
    }

    // Verifica l'interazione con una lega in cui l'utente è admin e la presenza delle opzioni amministrative.
    @Test
    public void testAdminLeagueInteraction() {
        sleep(1000);
        verifyThat("Lega Admin", isVisible());
        clickOn("Lega Admin"); // Questo apre LeagueAdminScreen
        
        // 1. Aspettiamo che la dashboard si carichi
        sleep(1000); 

        // 2. Clicchiamo sul MenuButton "⋮" per mostrare le voci
        clickOn("⋮");

        // 3. Verifichiamo che la voce "Richieste" sia presente nel menu a tendina
        // Usiamo il testo esatto definito nell'FXML
        verifyThat("#richiesteMenuItem", isVisible());
    }

    // Verifica che un partecipante non veda le opzioni amministrative nella lega selezionata.
    @Test
    public void testParticipantLeagueInteraction() {
        sleep(1000);
        verifyThat("Lega Partecipante", isVisible());

        // Verifica che il badge admin NON sia visibile su questa lega
        // (Il badge è visibile solo se isAdmin è true nel LeagueListItemController)
        long adminBadgesCount = lookup("#adminBadgeContainer").queryAll().stream()
                .filter(Node::isVisible)
                .count();
        assertEquals("Dovrebbe esserci 1 solo badge admin (quello della prima lega)", 1, adminBadgesCount);

        clickOn("Lega Partecipante"); // Questo apre LeagueScreen (User)
        
        sleep(1000);
        clickOn("⋮");

        // Verifichiamo che un utente normale NON veda le opzioni admin
        // Il robot non dovrebbe trovare il testo "Impostazioni Lega"
        try {
            verifyThat("#impostazioniLegaMenuItem", isVisible());
            fail("Un partecipante non dovrebbe vedere le impostazioni admin!");
        } catch (org.testfx.service.query.EmptyNodeQueryException e) {
            // Successo: il nodo non esiste per il partecipante
        }
    }
}