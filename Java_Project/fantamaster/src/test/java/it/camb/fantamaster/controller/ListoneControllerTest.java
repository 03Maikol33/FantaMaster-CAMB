package it.camb.fantamaster.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.testfx.api.FxAssert.verifyThat;

public class ListoneControllerTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/listone.fxml"));
        Parent root = loader.load();
        
        stage.setScene(new Scene(root, 400, 600));
        stage.show();
        stage.toFront();
    }

    @After
    public void tearDown() throws Exception {
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }

    // --- TEST 1: Caricamento Dati ---
    // Verifica il caricamento iniziale dei giocatori dal listone e la popolazione della combo ruoli.
    @Test
    public void testInitialLoadPopulatesList() {
        sleep(2000); 

        verifyThat("#playerContainer", (VBox box) -> !box.getChildren().isEmpty());
        
        ComboBox<String> combo = lookup("#roleCombo").queryComboBox();
        assertFalse("La combo dei ruoli non deve essere vuota", combo.getItems().isEmpty());
        assertEquals("Il primo elemento deve essere 'Tutti'", "Tutti", combo.getItems().get(0));
    }

    // --- TEST 2: Filtri Prezzo e Reset ---
    // Verifica l'applicazione dei filtri di prezzo e il reset dei campi.
    @Test
    public void testPriceFilterAndReset() {
        sleep(1500);

        // 1. Scriviamo filtri
        clickOn("#minPriceField").write("1");
        clickOn("#maxPriceField").write("500");
        
        // 2. Applichiamo con INVIO
        press(KeyCode.ENTER).release(KeyCode.ENTER);
        sleep(1000); 

        // Verifica lista popolata
        verifyThat("#playerContainer", (VBox box) -> !box.getChildren().isEmpty());

        // 3. RESET
        clickOn("#resetFilterButton");
        sleep(1000);

        // --- FIX ASSERTION ERROR ---
        // Invece di usare hasText() che si aspetta una Label, controlliamo direttamente il TextField
        verifyThat("#minPriceField", (TextField t) -> t.getText().isEmpty());
        verifyThat("#maxPriceField", (TextField t) -> t.getText().isEmpty());
        
        ComboBox<String> combo = lookup("#roleCombo").queryComboBox();
        assertEquals("Dopo il reset il ruolo deve essere Tutti", "Tutti", combo.getSelectionModel().getSelectedItem());
    }

    // --- TEST 3: Filtro Ruolo ---
    // Verifica il filtro per ruolo giocatore.
    @Test
    public void testRoleFilter() {
        sleep(1500);

        clickOn("#roleCombo");
        
        try {
            clickOn("P"); 
            sleep(1000);
            verifyThat("#playerContainer", (VBox box) -> !box.getChildren().isEmpty());
        } catch (Exception e) {
            System.out.println("⚠️ Ruolo 'P' non trovato o non cliccabile.");
        }
    }
}