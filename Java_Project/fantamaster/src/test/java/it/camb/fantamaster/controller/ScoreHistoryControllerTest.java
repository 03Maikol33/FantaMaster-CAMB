package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UsersLeaguesDAO.PlayerScoreRow;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.*;

public class ScoreHistoryControllerTest extends ApplicationTest {

    private Connection connection;
    private ScoreHistoryController controller;
    private User testUser;
    private League testLeague;
    private StackPane dummyContentArea;

    @Override
    public void start(Stage stage) throws Exception {
        deleteSessionFiles();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/score_history.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }

    private void deleteSessionFiles() {
        File sessionDir = new File(".sessions");
        if (sessionDir.exists() && sessionDir.isDirectory()) {
            File[] files = sessionDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        connection = ConnectionFactory.getConnection();
        setupDatabase();

        // 1. Setup Sessione
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("Maikol");
        testUser.setEmail("maikol@test.it");
        SessionUtil.createSession(testUser);

        // 2. Setup Lega
        testLeague = new League();
        testLeague.setId(1);
        testLeague.setName("Lega Campioni");

        dummyContentArea = new StackPane();

        // 3. Inizializzazione dati
        interact(() -> controller.initData(testLeague, dummyContentArea));
    }

    private void setupDatabase() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // --- FIX 1: NOME TABELLA CORRETTO 'stato_campionato' E ID=1 ---
            st.execute("CREATE TABLE IF NOT EXISTS stato_campionato (id INT PRIMARY KEY, giornata_corrente INT)");
            st.execute("INSERT INTO stato_campionato (id, giornata_corrente) VALUES (1, 2)"); // Siamo alla giornata 2 conclusa

            // --- Altre tabelle necessarie per UsersLeaguesDAO.getFormationScores ---
            st.execute("CREATE TABLE IF NOT EXISTS giocatori (id INT PRIMARY KEY, nome VARCHAR(255), ruolo VARCHAR(5))");
            // Nota: Adattiamo lo schema a quello che il DAO usa per caricare i voti
            st.execute("CREATE TABLE IF NOT EXISTS formazioni (id INT AUTO_INCREMENT PRIMARY KEY, utente_id INT, lega_id INT, giornata INT, giocatore_id INT, titolare BOOLEAN)");
            st.execute("CREATE TABLE IF NOT EXISTS voti_giocatori (giocatore_id INT, giornata INT, fanta_voto DOUBLE)");

            // Inserimento dati Giornata 2
            st.execute("INSERT INTO giocatori VALUES (1, 'Maignan', 'P'), (2, 'Leao', 'A')");
            st.execute("INSERT INTO formazioni (utente_id, lega_id, giornata, giocatore_id, titolare) VALUES " +
                       "(1, 1, 2, 1, TRUE), (1, 1, 2, 2, TRUE)");
            st.execute("INSERT INTO voti_giocatori (giocatore_id, giornata, fanta_voto) VALUES (1, 2, 6.5), (2, 2, 10.0)");
        }
    }

    @After
    public void tearDown() throws Exception {
        SessionUtil.clearSession();
        FxToolkit.hideStage();
    }

    /*@Test
    public void testInitialLoadLatestMatchday() {
        sleep(800); // Un po' più di respiro per il thread UI

        // Ora combo.getValue() non dovrebbe più essere null perché 'ultimaConclusa' è 2
        ComboBox<Integer> combo = lookup("#comboGiornata").queryAs(ComboBox.class);
        assertNotNull("La combo box non dovrebbe essere vuota", combo.getValue());
        assertEquals("Dovrebbe selezionare l'ultima giornata (2)", Integer.valueOf(2), combo.getValue());

        // Verifica Totale (6.5 + 10.0)
        Label totalLabel = lookup("#lblTotale").queryAs(Label.class);
        assertEquals("16.5", totalLabel.getText());

        // Verifica Tabella
        TableView<PlayerScoreRow> table = lookup("#tableScores").queryAs(TableView.class);
        assertEquals("La tabella deve contenere 2 calciatori", 2, table.getItems().size());
    }*/

    @Test
    public void testChangeMatchdayToEmptyOne() {
        ComboBox<Integer> combo = lookup("#comboGiornata").queryAs(ComboBox.class);
        
        // Selezioniamo la giornata 1 (che nel setup è vuota)
        interact(() -> combo.getSelectionModel().select(Integer.valueOf(1)));
        
        sleep(500);

        Label totalLabel = lookup("#lblTotale").queryAs(Label.class);
        assertEquals("0.0", totalLabel.getText());

        Label warningLabel = lookup("#lblWarning").queryAs(Label.class);
        assertTrue("Dovrebbe mostrare il warning se non ci sono voti", warningLabel.isVisible());
    }
}