package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.RulesDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane; 
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class LeagueAdminSettingsControllerTest extends ApplicationTest {

    private Connection connection;
    private User adminUser;
    private League testLeague;
    private LeagueAdminSettingsController controller;

    @Override
    public void start(Stage stage) throws Exception {
        Main.setPrimaryStage(stage);
        connection = ConnectionFactory.getConnection();
        setupDatabase();
        createTestData();
        SessionUtil.createSession(adminUser);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueAdminSettings.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setCurrentLeague(testLeague);
        
        stage.setScene(new Scene(root, 400, 700));
        stage.show();
    }

    private void setupDatabase() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("CREATE TABLE utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), asta_aperta BOOLEAN DEFAULT FALSE, turno_asta_utente_id INT, giocatore_chiamato_id INT, budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
            // Schema completo per la tabella regole
            stmt.execute("CREATE TABLE regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500, bonus_gol DOUBLE DEFAULT 3.0, bonus_assist DOUBLE DEFAULT 1.0, bonus_rigore_parato DOUBLE DEFAULT 3.0, bonus_imbattibilita DOUBLE DEFAULT 1.0, bonus_fattore_campo DOUBLE DEFAULT 2.0, malus_gol_subito DOUBLE DEFAULT 1.0, malus_autogol DOUBLE DEFAULT 2.0, malus_rigore_sbagliato DOUBLE DEFAULT 3.0, malus_espulsione DOUBLE DEFAULT 1.0, malus_ammonizione DOUBLE DEFAULT 0.5, usa_modificatore_difesa BOOLEAN DEFAULT FALSE)");
        }
    }

    private void createTestData() throws Exception {
        UserDAO userDAO = new UserDAO(connection);
        adminUser = new User(); adminUser.setUsername("Admin"); adminUser.setEmail("a@t.com"); adminUser.setHashPassword("x");
        userDAO.insert(adminUser);

        testLeague = new League("TestSettings", null, 10, adminUser, "punti_totali", LocalDateTime.now());
        new LeagueDAO(connection).insertLeague(testLeague);
        new RulesDAO(connection).insertDefaultRules(testLeague.getId());
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    public void testUpdateRules() {
        // Pulizia e inserimento nuovo valore budget
        doubleClickOn("#budgetField").write("7000"); 
        scroll(1.0); 
        clickOn("#saveRulesButton");
        clickOn("OK"); 

        Rules updated = new RulesDAO(connection).getRulesByLeagueId(testLeague.getId());
        assertEquals(7000, updated.getInitialBudget());
    }

    private void scroll(double v) {
        interact(() -> lookup("#leagueAdminSettingsScroll").queryAs(ScrollPane.class).setVvalue(v));
    }
}