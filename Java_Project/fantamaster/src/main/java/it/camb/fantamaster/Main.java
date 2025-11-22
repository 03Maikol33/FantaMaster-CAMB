package it.camb.fantamaster;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import it.camb.fantamaster.util.ConnectionFactory;
import java.sql.Connection;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLogin();
    }

    public static void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/mainScreen.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            System.out.println("Connessione al database riuscita!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);

    }
}