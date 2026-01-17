package it.camb.fantamaster.controller;

import it.camb.fantamaster.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class FantallenatoreListItemControllerTest extends ApplicationTest {

    private FantallenatoreListItemController controller;
    private User testUser;

    @Override
    public void start(Stage stage) throws Exception {
        // Carichiamo l'FXML specifico dell'item della lista
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fantallenatoreListItem.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        // Prepariamo un utente di test
        testUser = new User();
        testUser.setId(99);
        testUser.setUsername("LucaTest");

        stage.setScene(new Scene(root));
        stage.show();
    }

    // Verifica la visualizzazione corretta di un utente normale con rosa non piena.
    @Test
    public void testSetDataNormalUser() {
        // Stato: 10 giocatori su 25, rosa NON piena, utente NON admin
        interact(() -> controller.setData(testUser, 10, 25, false, false, user -> {}));

        // Verifica Username
        Label nameLabel = lookup("#usernameLabel").queryAs(Label.class);
        assertEquals("LucaTest", nameLabel.getText());

        // Verifica Slot
        Label slotsLabel = lookup("#slotsLabel").queryAs(Label.class);
        assertEquals("10/25", slotsLabel.getText());

        // Verifica Indicatore Stato (Verde se disponibile)
        Circle status = lookup("#statusIndicator").queryAs(Circle.class);
        assertEquals(Color.web("#48bb78"), status.getFill());

        // Il bottone non deve essere visibile per i non-admin
        Button btn = lookup("#assignTurnButton").queryAs(Button.class);
        assertFalse("Il bottone non deve essere visibile se non admin", btn.isVisible());
    }

    // Verifica che una rosa piena sia indicata visivamente con opacità e indicatore rosso.
    @Test
    public void testSetDataRosterFull() {
        // Stato: Rosa piena
        interact(() -> controller.setData(testUser, 25, 25, true, false, user -> {}));

        // L'opacità del container deve essere dimezzata
        HBox container = lookup("#itemContainer").queryAs(HBox.class);
        assertEquals(0.5, container.getOpacity(), 0.01);

        // L'indicatore deve essere rosso
        Circle status = lookup("#statusIndicator").queryAs(Circle.class);
        assertEquals(Color.web("#e53e3e"), status.getFill());
        
        // Il bottone deve essere sparito anche se fossimo admin (logica isRosterFull prioritaria)
        Button btn = lookup("#assignTurnButton").queryAs(Button.class);
        assertFalse(btn.isVisible());
    }

    // Verifica che l'admin veda il bottone di assegnazione turno e che la callback sia eseguita al click.
    @Test
    public void testAdminActionCallback() {
        // Usiamo un AtomicReference per catturare l'utente passato alla callback
        AtomicReference<User> capturedUser = new AtomicReference<>();
        
        // Stato: Utente Admin
        interact(() -> controller.setData(testUser, 5, 25, false, true, capturedUser::set));

        Button btn = lookup("#assignTurnButton").queryAs(Button.class);
        assertTrue("Il bottone deve essere visibile per l'admin", btn.isVisible());

        // Clicchiamo sul bottone per scatenare la callback
        clickOn(btn);

        // Verifichiamo che la callback abbia ricevuto l'utente corretto
        assertNotNull("La callback deve essere stata eseguita", capturedUser.get());
        assertEquals(testUser.getUsername(), capturedUser.get().getUsername());
    }
}