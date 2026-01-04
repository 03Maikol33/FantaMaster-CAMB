package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.LeagueDAO;
import it.camb.fantamaster.dao.MessageDAO;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Message;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class ChatViewControllerTest extends ApplicationTest {

    private Connection connection;
    private User testUser;
    private League testLeague;
    
    // Riferimento al controller per poter chiamare stopPolling()
    private ChatViewController controller; 

    @Override
    public void start(Stage stage) throws Exception {
        setupDatabase();
        createTestData();

        // 1. Simuliamo Login
        SessionUtil.createSession(testUser);

        // 2. Carichiamo l'FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatView.fxml"));
        Parent root = loader.load();
        
        // 3. RECUPERO CONTROLLER E AVVIO CHAT
        // Questo è fondamentale: initData avvia il timer di polling!
        controller = loader.getController();
        controller.initData(testLeague);
        
        stage.setScene(new Scene(root, 400, 600));
        stage.show();
        stage.toFront();
    }

    private void setupDatabase() throws Exception {
        connection = ConnectionFactory.getConnection();
        try (Statement stmt = connection.createStatement()) {
            // Tabelle necessarie per la chat e le relazioni
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255), hash_password VARCHAR(255), created_at TIMESTAMP, avatar BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS leghe (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), icona BLOB, max_membri INT, id_creatore INT, iscrizioni_chiuse BOOLEAN, created_at TIMESTAMP, codice_invito VARCHAR(255), modalita VARCHAR(50), moduli_consentiti VARCHAR(255), budget_iniziale INT DEFAULT 500)");
            stmt.execute("CREATE TABLE IF NOT EXISTS utenti_leghe (utente_id INT, lega_id INT, PRIMARY KEY(utente_id, lega_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS regole (id INT AUTO_INCREMENT PRIMARY KEY, lega_id INT, budget_iniziale INT DEFAULT 500)");
            // Tabella Messaggi
            stmt.execute("CREATE TABLE IF NOT EXISTS messaggi (id INT AUTO_INCREMENT PRIMARY KEY, testo TEXT, data_invio TIMESTAMP, utente_id INT, lega_id INT)");
        }
    }

    private void createTestData() {
        UserDAO userDAO = new UserDAO(connection);
        LeagueDAO leagueDAO = new LeagueDAO(connection);
        MessageDAO messageDAO = new MessageDAO(connection);

        // Utente
        testUser = new User();
        testUser.setUsername("ChatterBox");
        testUser.setEmail("chat@test.com");
        testUser.setHashPassword("x");
        userDAO.insert(testUser);

        // Lega
        testLeague = new League();
        testLeague.setName("Lega Chat");
        testLeague.setMaxMembers(10);
        testLeague.setCreator(testUser);
        testLeague.setCreatedAt(LocalDateTime.now());
        testLeague.setParticipants(new ArrayList<>());
        leagueDAO.insertLeague(testLeague);

        // Messaggio iniziale per verificare il caricamento all'avvio
        Message oldMsg = new Message("Benvenuto nella chat!", testUser, testLeague.getId());
        messageDAO.insertMessage(oldMsg);
    }

    @After
    public void tearDown() throws Exception {
        // --- STOP POLLING ---
        // Fondamentale per evitare che il test rimanga appeso o lanci errori su connessioni chiuse
        if (controller != null) {
            controller.stopPolling();
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE messaggi");
            stmt.execute("DROP TABLE regole");
            stmt.execute("DROP TABLE utenti_leghe");
            stmt.execute("DROP TABLE leghe");
            stmt.execute("DROP TABLE utenti");
        }
        SessionUtil.deleteSession("chat@test.com");
        
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }

    // --- I TEST ---

    @Test
    public void testInitialMessagesAreLoaded() {
        // La chat è asincrona, diamo tempo al loadMessages() iniziale di finire
        sleep(1000);

        // Verifica: Il messaggio pre-inserito deve essere visibile
        verifyThat("Benvenuto nella chat!", isVisible());
    }

    @Test
    public void testSendMessage() {
        sleep(1000); // Wait init

        // 1. Scrivo il messaggio nel TextField (fx:id="inputField")
        clickOn("#inputField").write("Messaggio TestFX");

        // 2. Invio con il bottone (fx:id="sendButton")
        clickOn("#sendButton");

        // 3. Attesa: Invio Async -> DB -> Polling/Refresh UI
        sleep(1500);

        // 4. Verifica UI: Il messaggio deve apparire nella VBox
        verifyThat("Messaggio TestFX", isVisible());

        // 5. Verifica UI: La casella di testo deve essersi svuotata
        verifyThat("#inputField", (TextField t) -> t.getText().isEmpty());
    }

    @Test
    public void testDeleteMessage() {
        // 1. Aspettiamo che il messaggio iniziale appaia
        sleep(1000);
        String msgText = "Benvenuto nella chat!";
        verifyThat(msgText, isVisible());

        // 2. CERCHIAMO E CLICCHIAMO IL TASTO ELIMINA
        // Strategia: Il controller crea un Button con dentro un'ImageView (senza testo)
        // oppure una Label con testo "❌". Proviamo entrambi i casi.
        try {
            // Caso A: L'icona è stata caricata -> Cerchiamo un bottone senza testo (l'unico altro bottone è "Invia ➤")
            clickOn((javafx.scene.Node node) -> 
                node instanceof javafx.scene.control.Button && 
                ((javafx.scene.control.Button) node).getText().isEmpty()
            );
        } catch (Exception e) {
            // Caso B: Fallback -> Cerchiamo la X
            clickOn("❌");
        }

        // 3. GESTIONE DELL'ALERT DI CONFERMA
        // Quando clicchi elimina, esce un Alert. TestFX deve cliccare "Sì" o "Yes".
        // ButtonType.YES viene localizzato dal sistema operativo.
        // Proviamo a cliccare il pulsante di default dell'alert o cerchiamo il testo.
        sleep(500); // Tempo che l'alert appaia
        press(KeyCode.ENTER).release(KeyCode.ENTER);

        // 4. ATTESA CANCELLAZIONE (Async DB + Refresh)
        sleep(1500);

        // 5. VERIFICA CHE IL MESSAGGIO SIA SPARITO
        // lookup(...) cerca nodi con quel testo. queryAll().size() deve essere 0.
        int messaggiTrovati = lookup(msgText).queryAll().size();
        
        // Usiamo un'asserzione standard di JUnit
        assertFalse("Il messaggio dovrebbe essere stato eliminato dalla vista", messaggiTrovati > 0);
        
        // 6. VERIFICA DB (Opzionale ma consigliata)
        MessageDAO dao = new MessageDAO(connection);
        boolean existsInDb = dao.getMessagesByLeagueId(testLeague.getId()).stream()
                .anyMatch(m -> m.getText().equals(msgText));
        assertFalse("Il messaggio deve essere sparito dal DB", existsInDb);
    }
}