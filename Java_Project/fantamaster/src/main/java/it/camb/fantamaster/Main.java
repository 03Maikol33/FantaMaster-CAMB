package it.camb.fantamaster;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import it.camb.fantamaster.controller.LeagueAdminScreenController;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        // Imposto cosa succede se chiudi la finestra principale con la "X"
        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Blocca la chiusura standard di JavaFX
            shutdown();  // Esegue la nostra chiusura pulita
        });

        // 1. Mostra la finestra di Login classica
        showLogin();

        // 2. Apre la finestra di servizio "Kill Switch"
        createKillSwitch();
    }

    /**
     * Crea una piccola finestra accessoria per chiudere tutto e liberare il DB.
     */
    private void createKillSwitch() {
        Stage utilityStage = new Stage();
        utilityStage.setTitle("Dev Tools");
        
        Button killButton = new Button("CHIUDI APP & DB");
        killButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Al click, esegue la procedura di spegnimento sicuro
        killButton.setOnAction(e -> shutdown());

        VBox layout = new VBox(killButton);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        Scene scene = new Scene(layout, 200, 100);
        utilityStage.setScene(scene);
        
        // Imposta la finestra in una posizione comoda (es. in alto a destra)
        utilityStage.setX(100);
        utilityStage.setY(100);
        utilityStage.setAlwaysOnTop(true); // Rimane sempre visibile sopra le altre
        utilityStage.show();
    }

    /**
     * Procedura di chiusura sicura.
     * Recupera la connessione Singleton e la chiude esplicitamente.
     */
    public static void shutdown() {
        System.out.println("Avvio procedura di chiusura...");
        try {
            // Recuperiamo l'istanza della connessione dalla Factory
            Connection conn = ConnectionFactory.getConnection();
            
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("✅ Connessione al Database CHIUSA con successo.");
            } else {
                System.out.println("⚠️ Nessuna connessione attiva da chiudere.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore durante la chiusura della connessione:");
            e.printStackTrace();
        }

        // Chiude l'applicazione JavaFX
        Platform.exit();
        // Termina forzatamente la JVM (assicura che tutti i thread muoiano)
        System.exit(0);
    }

    // --- Metodi di Navigazione Standard ---

    public static void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("FantaMaster - Login");
        primaryStage.show();
    }

    public static void showHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/mainScreen.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("FantaMaster - Home");
        primaryStage.centerOnScreen(); // Centra la finestra
        primaryStage.show();
    }

    public static void showLeagueAdminScreen(League league) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/leagueAdminScreen.fxml"));
        Parent root = loader.load();
        LeagueAdminScreenController controller = loader.getController();
        controller.setCurrentLeague(league);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void showLeagueScreen(League league) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/leagueScreen.fxml"));
        Parent root = loader.load();
        // Nota: Assumo che tu usi lo stesso controller o un altro simile.
        // Se è LeagueScreenController, cambia il cast qui sotto.
        LeagueAdminScreenController controller = loader.getController();
        controller.setCurrentLeague(league);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Questo test nel main è utile per debug, ma la vera connessione
        // persistente viene gestita dai controller durante l'uso dell'app.
        try (Connection conn = ConnectionFactory.getConnection()) {
            System.out.println("Test iniziale: Connessione al database riuscita!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        launch(args);
    }
}