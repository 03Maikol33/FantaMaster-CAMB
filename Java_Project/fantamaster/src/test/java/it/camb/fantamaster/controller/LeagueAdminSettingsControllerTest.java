package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RulesDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane; // Importante
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

public class LeagueAdminSettingsControllerTest extends ApplicationTest {

    private Connection connection;
    private User adminUser;
    private League testLeague;
    private LeagueAdminSettingsController controller;

    @Override
    public void start(Stage stage) throws Exception {
        Main.setPrimaryStage(stage);
        
        setupDatabase();
        createTestData();
        SessionUtil.createSession(adminUser);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueAdminSettings.fxml"));
        Parent root = loader.load();
        
        controller = loader.getController();
        controller.setCurrentLeague(testLeague);
        
        stage.setScene(new Scene(root, 400, 700));
        stage.show();
        stage.toFront();
    }

    private void setupDatabase() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500, bonus_gol DOUBLE DEFAULT 3.0, bonus_assist DOUBLE DEFAULT 1.0, bonus_rigore_parato DOUBLE DEFAULT 3.0, bonus_imbattibilita DOUBLE DEFAULT 1.0, bonus_fattore_campo DOUBLE DEFAULT 2.0, malus_gol_subito DOUBLE DEFAULT 1.0, malus_autogol DOUBLE DEFAULT 2.0, malus_rigore_sbagliato DOUBLE DEFAULT 3.0, malus_espulsione DOUBLE DEFAULT 1.0, malus_ammonizione DOUBLE DEFAULT 0.5, usa_modificatore_difesa BOOLEAN DEFAULT FALSE)");
        }
    }

    private void createTestData() throws Exception {
        UserDAO userDAO = new UserDAO(connection);
        LeagueDAO leagueDAO = new LeagueDAO(connection);

        adminUser = new User();
        adminUser.setUsername("AdminLega");
        adminUser.setEmail("admin@test.com");
        adminUser.setHashPassword("x");
        userDAO.insert(adminUser);

        testLeague = new League();
        testLeague.setName("Lega Settings");
        testLeague.setMaxMembers(10);
        testLeague.setCreator(adminUser);
        testLeague.setCreatedAt(LocalDateTime.now());
        testLeague.setParticipants(new ArrayList<>());
        testLeague.getParticipants().add(adminUser);
        testLeague.setAllowedFormations("4-4-2,3-5-2");
        leagueDAO.insertLeague(testLeague);
        
        String sqlRules = "INSERT INTO regole (lega_id, budget_iniziale) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sqlRules)) {
            ps.setInt(1, testLeague.getId());
            ps.setInt(2, 500);
            ps.executeUpdate();
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
        SessionUtil.deleteSession("admin@test.com");
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }

    // --- HELPER METODO PER SCROLLARE ---
    private void scroll(double vValue) {
        // interact esegue il codice nel thread JavaFX (obbligatorio per modificare la UI)
        interact(() -> {
            // Cerchiamo la ScrollPane e impostiamo la posizione verticale
            // 0.0 = Top, 1.0 = Bottom
            lookup("#leagueAdminSettingsScroll").queryAs(ScrollPane.class).setVvalue(vValue);
        });
        sleep(200); // Piccola pausa per dare tempo all'animazione
    }

    // --- TEST 1: Modifica e Salvataggio Regole ---
    @Test
    public void testUpdateRules() {
        sleep(1000);
        scroll(0.0); // Assicuriamoci di essere in cima

        // Modifica Budget
        clickOn("#budgetField").write("0"); 
        doubleClickOn("#bonusGolField").write("5.0");

        // Modifica Moduli (sono in alto)
        clickOn("4-3-3");

        // Salva (è in basso)
        scroll(1.0); // Scrolliamo in fondo
        clickOn("#saveRulesButton");
        
        sleep(500);
        press(KeyCode.ENTER).release(KeyCode.ENTER); 

        // Verifica DB
        RulesDAO rulesDAO = new RulesDAO(connection);
        LeagueDAO leagueDAO = new LeagueDAO(connection);
        Rules updatedRules = rulesDAO.getRulesByLeagueId(testLeague.getId());
        League updatedLeague = leagueDAO.getLeagueById(testLeague.getId());

        assertEquals(5000, updatedRules.getInitialBudget());
        assertEquals(5.0, updatedRules.getBonusGol(), 0.01);
        assertTrue(updatedLeague.getAllowedFormations().contains("4-3-3"));
    }

    // --- TEST 2: Chiusura Iscrizioni ---
    @Test
    public void testCloseRegistrationsLogic() {
        sleep(1000);
        scroll(1.0); // Andiamo subito in fondo alla zona pericolosa

        // A. Dispari -> Disabilitato
        verifyThat("#closeRegistrationsButton", isDisabled());
        
        // B. Pari -> Abilitato
        UserDAO userDAO = new UserDAO(connection);
        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(connection);
        User user2 = new User(); user2.setUsername("User2"); user2.setEmail("u2@test.com"); user2.setHashPassword("pass");
        userDAO.insert(user2);
        ulDAO.subscribeUserToLeague(user2, testLeague);
        
        // Refresh manuale
        interact(() -> controller.setCurrentLeague(testLeague)); 
        sleep(500);
        
        // Forziamo il refresh visivo scrollando su e giù
        scroll(0.0);
        scroll(1.0);

        verifyThat("#closeRegistrationsButton", isEnabled());
        
        clickOn("#closeRegistrationsButton");
        sleep(500);
        press(KeyCode.ENTER).release(KeyCode.ENTER);

        LeagueDAO leagueDAO = new LeagueDAO(connection);
        League l = leagueDAO.getLeagueById(testLeague.getId());
        assertTrue(l.isRegistrationsClosed());
    }

    // --- TEST 3: Eliminazione Lega ---
    @Test
    public void testDeleteLeague() {
        sleep(1000);
        scroll(1.0); // Andiamo in fondo

        clickOn("#deleteLeagueButton");
        
        sleep(500);
        press(KeyCode.ENTER).release(KeyCode.ENTER);

        LeagueDAO leagueDAO = new LeagueDAO(connection);
        League deletedLeague = leagueDAO.getLeagueById(testLeague.getId());
        
        assertNull(deletedLeague);
    }
}