package it.camb.fantamaster;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import it.camb.fantamaster.controller.LeagueAdminScreenController;
import it.camb.fantamaster.controller.LeagueScreenController;
import it.camb.fantamaster.dao.CampionatoDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    private static Stage primaryStage;

    public static Stage getPrimaryStage() {return primaryStage;}
    public static void setPrimaryStage(Stage stage) {primaryStage = stage;}

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

        // 2. Apre la finestra di servizio "Kill Switch" (Dev Tools)
        createKillSwitch();
    }

    /**
     * Crea una piccola finestra accessoria per chiudere tutto e gestire il campionato.
     */
    private void createKillSwitch() {
        Stage utilityStage = new Stage();
        utilityStage.setTitle("Dev Tools");
        
        // Bottone originale Chiusura
        Button killButton = new Button("CHIUDI APP & DB");
        killButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        killButton.setMaxWidth(Double.MAX_VALUE);
        killButton.setOnAction(e -> shutdown());

        // NUOVO: Bottone Gestione Giornate nella finestra esterna
        Button manageButton = new Button("GESTIONE GIORNATE");
        manageButton.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        manageButton.setMaxWidth(Double.MAX_VALUE);
        manageButton.setOnAction(e -> {
            try {
                CampionatoDAO dao = new CampionatoDAO(ConnectionFactory.getConnection());
                int attuale = dao.getGiornataCorrente();

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Gestione Campionato");
                alert.setHeaderText("Giornata Sbloccata nel DB: " + attuale);
                alert.setContentText("Cosa vuoi fare con le giornate simulate?");

                ButtonType btnAvanza = new ButtonType("Sblocca Prossima");
                ButtonType btnReset = new ButtonType("Reset a 0");
                ButtonType btnAnnulla = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(btnAvanza, btnReset, btnAnnulla);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == btnAvanza) {
                        dao.avanzaGiornata();
                        new Alert(Alert.AlertType.INFORMATION, "Successo! Sbloccata giornata " + dao.getGiornataCorrente()).show();
                    } else if (result.get() == btnReset) {
                        // Reset manuale
                        java.sql.PreparedStatement st = ConnectionFactory.getConnection().prepareStatement("UPDATE stato_campionato SET giornata_corrente = 0 WHERE id = 1");
                        st.executeUpdate();
                        new Alert(Alert.AlertType.INFORMATION, "Campionato resettato a 0.").show();
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Errore connessione DB.").show();
            }
        });

        VBox layout = new VBox(15, killButton, manageButton);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        Scene scene = new Scene(layout, 250, 160);
        utilityStage.setScene(scene);
        
        utilityStage.setX(100);
        utilityStage.setY(100);
        utilityStage.setAlwaysOnTop(true);
        utilityStage.show();
    }

    public static void shutdown() {
        System.out.println("Avvio procedura di chiusura...");
        try {
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
        Platform.exit();
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
        primaryStage.centerOnScreen();
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
        LeagueScreenController controller = loader.getController();
        controller.setCurrentLeague(league);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            System.out.println("Test iniziale: Connessione al database riuscita!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }
}