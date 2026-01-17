package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

public class LeagueRulesControllerTest extends ApplicationTest {

    private Connection connection;
    private LeagueRulesController controller;
    private League testLeague;

    @Override
    public void start(Stage stage) throws Exception {
        // Carichiamo l'interfaccia FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueRules.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        
        stage.setScene(new Scene(root, 335, 600));
        stage.show();
    }

    @Before
    public void setUp() throws Exception {
        // Otteniamo la connessione condivisa per H2
        connection = ConnectionFactory.getConnection();
        
        try (Statement st = connection.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Creazione tabella leghe (allineata ai nomi colonne usati nel progetto)
            st.execute("CREATE TABLE IF NOT EXISTS leghe (" +
                    "id INT PRIMARY KEY, " +
                    "nome VARCHAR(255), " +
                    "moduli_consentiti VARCHAR(255))");

            // Inseriamo una lega con alcuni moduli già attivi
            st.execute("INSERT INTO leghe (id, nome, moduli_consentiti) VALUES (1, 'Lega Test', '4-4-2,4-3-3')");
        }

        // Prepariamo l'oggetto League per il controller
        testLeague = new League();
        testLeague.setId(1);
        testLeague.setAllowedFormations("4-4-2,4-3-3");

        // Passiamo la lega al controller tramite interact() per garantire la sicurezza del thread UI
        interact(() -> controller.setCurrentLeague(testLeague));
    }

    @After
    public void tearDown() throws Exception {
        SessionUtil.clearSession();
        FxToolkit.hideStage();
    }

    @Test
    public void testInitialLoadAndPreSelection() {
        // Verifica che siano stati creati i 7 moduli standard
        int toggleCount = lookup(".toggle-button").queryAll().size();
        assertEquals("Dovrebbero esserci 7 pulsanti per i moduli", 7, toggleCount);

        // Verifica che i moduli salvati nel DB siano selezionati
        ToggleButton btn442 = lookup("4-4-2").queryAs(ToggleButton.class);
        ToggleButton btn343 = lookup("3-4-3").queryAs(ToggleButton.class);

        assertTrue("Il modulo 4-4-2 dovrebbe essere selezionato all'avvio", btn442.isSelected());
        assertFalse("Il modulo 3-4-3 non dovrebbe essere selezionato all'avvio", btn343.isSelected());
    }

    @Test
    public void testValidationFailureWhenNoModulesSelected() {
        // Deselezioniamo tutti i moduli
        interact(() -> {
            lookup(".toggle-button").queryAll().forEach(node -> ((ToggleButton) node).setSelected(false));
        });

        // Tentativo di salvataggio
        clickOn("#saveButton");

        // Verifica visibilità etichetta errore
        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertTrue("L'errore dovrebbe essere visibile se non ci sono moduli selezionati", errorLabel.isVisible());
    }

    @Test
    public void testSaveRulesSuccess() throws Exception {
        // Selezioniamo il modulo 3-4-3 (aggiungendolo a quelli esistenti o resettando)
        clickOn("3-4-3");
        
        // Salviamo le regole
        clickOn("#saveButton");

        // Gestione dell'Alert di successo (attesa e click su OK)
        sleep(500);
        clickOn("OK");

        // Verifichiamo che il DB H2 sia stato aggiornato correttamente
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT moduli_consentiti FROM leghe WHERE id = 1")) {
            assertTrue(rs.next());
            String dbRules = rs.getString(1);
            assertTrue("Il DB dovrebbe contenere il modulo 3-4-3", dbRules.contains("3-4-3"));
        }
        
        // Verifica che il modello League sia stato aggiornato
        assertTrue("Il modello locale deve contenere 3-4-3", testLeague.getAllowedFormations().contains("3-4-3"));
    }

    @Test
    public void testDatabaseErrorHandling() throws Exception {
        // Forza un errore eliminando la tabella prima del salvataggio
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE leghe");
        }

        clickOn("#saveButton");

        // Verifica che appaia l'alert di errore ("Errore di connessione.")
        sleep(500);
        clickOn("OK");
    }
}