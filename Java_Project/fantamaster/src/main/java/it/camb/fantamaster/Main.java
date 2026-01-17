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
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.CampionatoUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20; -fx-background-color: #f4f4f4;");

        Runnable refreshUI = () -> {
            layout.getChildren().clear();
            
            Button killButton = new Button("CHIUDI APP & DB");
            killButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold;");
            killButton.setMaxWidth(Double.MAX_VALUE);
            killButton.setOnAction(e -> shutdown());
            layout.getChildren().add(killButton);

            try {
                CampionatoDAO dao = new CampionatoDAO(ConnectionFactory.getConnection());
                
                if (!dao.existsStatoCampionato()) {
                    Button initBtn = new Button("INIZIALIZZA CAMPIONATO");
                    initBtn.setStyle("-fx-background-color: #2b6cb0; -fx-text-fill: white;");
                    initBtn.setMaxWidth(Double.MAX_VALUE);
                    initBtn.setOnAction(e -> {
                        try { dao.inizializzaCampionato(); utilityStage.close(); createKillSwitch(); } 
                        catch (SQLException ex) {
                            ErrorUtil.log("Errore DB", ex);
                        }
                    });
                    layout.getChildren().add(initBtn);
                } else {
                    int passate = dao.getGiornataCorrente();
                    int daGiocare = passate + 1;

                    CampionatoUtil.load("/api/campionato.json");
                    boolean esisteInJson = !CampionatoUtil.getMatchesByDay(daGiocare).isEmpty();

                    if (esisteInJson) {
                        if (!dao.existsGiornataFisica(daGiocare)) {
                            Button progBtn = new Button("PROGRAMMA GIORNATA " + daGiocare);
                            progBtn.setStyle("-fx-background-color: #38a169; -fx-text-fill: white;");
                            progBtn.setMaxWidth(Double.MAX_VALUE);
                            progBtn.setOnAction(e -> {
                                try { dao.programmaGiornata(daGiocare); utilityStage.close(); createKillSwitch(); } 
                                catch (SQLException ex) { 
                                    ErrorUtil.log("Errore DB", ex);
                                 }
                            });
                            layout.getChildren().add(progBtn);
                        } else {
                            Button execBtn = new Button("ESEGUI " + daGiocare + " E APRI " + (daGiocare + 1));
                            execBtn.setStyle("-fx-background-color: #d69e2e; -fx-text-fill: white;");
                            execBtn.setMaxWidth(Double.MAX_VALUE);
                            execBtn.setOnAction(e -> {
                                try { dao.eseguiGiornataEProgrammaSuccessiva(daGiocare); utilityStage.close(); createKillSwitch(); } 
                                catch (SQLException ex) { 
                                    ErrorUtil.log("Errore DB", ex);
                                 }
                            });
                            layout.getChildren().add(execBtn);
                        }
                    } else {
                        Label fin = new Label("🏆 CAMPIONATO CONCLUSO");
                        fin.setStyle("-fx-text-fill: #2b6cb0; -fx-font-weight: bold;");
                        layout.getChildren().add(fin);
                    }

                    Button resetBtn = new Button("RESET TOTALE");
                    resetBtn.setStyle("-fx-background-color: #718096; -fx-text-fill: white;");
                    resetBtn.setMaxWidth(Double.MAX_VALUE);
                    resetBtn.setOnAction(e -> {
                        try { dao.inizializzaCampionato(); utilityStage.close(); createKillSwitch(); } 
                        catch (SQLException ex) { 
                            ErrorUtil.log("Errore DB", ex);
                         }
                    });
                    layout.getChildren().add(resetBtn);
                }
            } catch (SQLException ex) {
                layout.getChildren().add(new Label("Errore DB: " + ex.getMessage()));
            }
        };

        refreshUI.run();
        Scene scene = new Scene(layout, 280, 260);
        utilityStage.setScene(scene);
        utilityStage.setX(50); utilityStage.setY(50);
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
            ErrorUtil.log("Errore chiusura DB", e);
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
        try {
            Connection conn = ConnectionFactory.getConnection();
            System.out.println("Test iniziale: Connessione al database riuscita!");
        } catch (Exception e) {
            ErrorUtil.log("Errore connessione DB all'avvio", e);
            System.err.println("Impossibile connettersi al database. L'applicazione verrà chiusa.");
            Platform.exit();
        }
        launch(args);
    }
}