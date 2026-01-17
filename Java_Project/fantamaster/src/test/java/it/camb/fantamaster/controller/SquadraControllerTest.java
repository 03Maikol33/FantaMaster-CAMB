package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.model.*;
import it.camb.fantamaster.util.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static org.junit.Assert.*;

public class SquadraControllerTest extends ApplicationTest {

    private Connection connection;
    private SquadraController controller;
    private RosaDAO rosaDAO;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/squadra.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }

    @Before
    public void setUp() throws Exception {
        connection = ConnectionFactory.getConnection();
        rosaDAO = new RosaDAO(connection);
        
        try (Statement st = connection.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            st.execute("DROP ALL OBJECTS");
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // 1. Creazione Tabelle con le colonne ESATTE richieste dal tuo RosaDAO
            st.execute("CREATE TABLE utenti (id INT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255))");
            st.execute("CREATE TABLE leghe (id INT PRIMARY KEY, nome VARCHAR(255))");
            st.execute("CREATE TABLE utenti_leghe (id INT PRIMARY KEY, utente_id INT, lega_id INT)");
            
            // Nota: colonna 'crediti_residui' come richiesto dal DAO
            st.execute("CREATE TABLE rosa (id INT PRIMARY KEY, utenti_leghe_id INT, nome_rosa VARCHAR(255), crediti_residui INT)");
            
            // Nota: colonne 'squadra_reale' e 'quotazione_iniziale' come richiesto dal DAO
            st.execute("CREATE TABLE giocatori (id INT PRIMARY KEY, nome VARCHAR(255), ruolo VARCHAR(5), squadra_reale VARCHAR(255), quotazione_iniziale INT)");
            
            st.execute("CREATE TABLE giocatori_rose (rosa_id INT, giocatore_id INT)");

            // 2. Inserimento dati di base
            st.execute("INSERT INTO utenti VALUES (1, 'Maikol', 'maikol@test.it')");
            st.execute("INSERT INTO leghe VALUES (1, 'Lega Serie A')");
            st.execute("INSERT INTO utenti_leghe VALUES (1, 1, 1)");
            
            // Inseriamo la rosa iniziale (usiamo SQL per il primo inserimento per avere l'ID 1)
            st.execute("INSERT INTO rosa VALUES (1, 1, 'Temporary Name', 100)");
            
            // Inseriamo un giocatore
            st.execute("INSERT INTO giocatori VALUES (17, 'Lautaro', 'A', 'Inter', 30)");
            st.execute("INSERT INTO giocatori_rose VALUES (1, 17)");
        }

        // 3. Uso del DAO per cambiare il nome (come hai chiesto tu!)
        rosaDAO.updateRosaInfo(1, "Dream Team");

        // Stampa di debug per vedere se il DB è popolato bene
        debugPrintTable("rosa");

        // 4. Setup Sessione
        User testUser = new User();
        testUser.setId(1);
        testUser.setEmail("maikol@test.it");
        SessionUtil.createSession(testUser);

        League testLeague = new League();
        testLeague.setId(1);

        // 5. Inizializzazione Controller
        interact(() -> controller.initData(testLeague));
    }

    @After
    public void tearDown() throws Exception {
        SessionUtil.clearSession();
        FxToolkit.hideStage();
    }

    @Test
    public void testSquadraCaricataCorrettamente() {
        sleep(500);

        // Verifica Nome Squadra aggiornato dal DAO
        Label nameLabel = lookup("#nomeSquadraLabel").queryAs(Label.class);
        assertEquals("Il nome della squadra dovrebbe essere 'Dream Team'", "Dream Team", nameLabel.getText());

        // Verifica Tabella
        TableView<Player> table = lookup("#giocatoriTable").queryAs(TableView.class);
        assertEquals("Dovrebbe esserci 1 giocatore", 1, table.getItems().size());
        
        Player p = table.getItems().get(0);
        assertEquals("Lautaro", p.getNome()); // Il tuo DAO mette cognome vuoto
    }

    /**
     * Utility per visualizzare le tuple del database H2 in console durante il test.
     */
    private void debugPrintTable(String tableName) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            System.out.println("\n--- DEBUG TABLE: " + tableName + " ---");
            while (rs.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    String columnValue = rs.getString(i);
                    System.out.print(rsmd.getColumnName(i) + ": " + columnValue);
                }
                System.out.println("");
            }
            System.out.println("-------------------------------\n");
        } catch (Exception e) {
            System.err.println("Errore debug DB: " + e.getMessage());
        }
    }
}