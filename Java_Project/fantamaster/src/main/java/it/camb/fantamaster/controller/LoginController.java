
package it.camb.fantamaster.controller;

import java.io.IOException;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.PasswordUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
/*
    @FXML
    private void handleLogin() throws IOException {
        //simulazione del login
        User user = checkUser();
        if(user != null) {
            //crea la sessione
            System.out.println("Test la sessione esiste già per l'utente?: " + SessionUtil.loadSession(user.getEmail()));
            SessionUtil.createSession(user);
            Main.showHome();
        } else {
            boolean validEmail = checkEmail(emailField.getText());
            boolean validPassword = checkPassword(passwordField.getText());
            String errorMessage = "";
            if (!validEmail) {
                errorMessage += "Email non valida. ";
            }
            if (!validPassword) {
                errorMessage += "Password non valida. ";
            }
            //mostra la error label
            errorLabel.setText(errorMessage);
            errorLabel.setVisible(true);
        }
    }*/
    @FXML
    private void initialize() {
        //deve aspettare che la UI sia caricata per fare l'auto login
        Platform.runLater(() -> autoLoginIfSessionExists());
    }

    @FXML
    private void handleLogin() throws IOException {
        boolean validEmail = checkEmail(emailField.getText());
        boolean validPassword = checkPassword(passwordField.getText());
        if (!validEmail || !validPassword) {
            //mostra la error label
            errorLabel.setText("Email o Password non valide.");
            errorLabel.visibleProperty().set(true);
            return;
        }
        User user = checkUser();
        if (user != null) {
            //crea la sessione
            System.out.println("Test la sessione esiste già per l'utente?: " + SessionUtil.loadSession(user.getEmail()));
            SessionUtil.createSession(user);
            openHomeScreen();
        }
    }

    @FXML
    private void openHomeScreen() throws IOException {
        Main.showHome();
    }

    @FXML
    private User checkUser() {
        String email = emailField.getText();
        String password = passwordField.getText();
        try{
            UserDAO userDAO = new UserDAO(ConnectionFactory.getConnection());

            User user = userDAO.findByEmail(email);
            if(user == null) {
                errorLabel.setText("Utente non trovato. Registrati.");
                errorLabel.visibleProperty().set(true);
                return null;
            }
            if(PasswordUtil.checkPassword(password, user.getHashPassword())) { //se la password è corretta
                return user; //ritorna l'utente
            }
            else{
                errorLabel.setText("La Password è errata.");
                errorLabel.visibleProperty().set(true);
                return null;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @FXML
    private void handleRegister() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) emailField.getScene().getWindow(); // recupera lo stage corrente
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void hideErrorLabel() {
        errorLabel.visibleProperty().set(false);
    }

    private boolean checkEmail(String email) {
        // Implementa la logica di validazione dell'email
        System.out.println("Email inserita: " + email);
        return email != null && email.contains("@");
    }
    private boolean checkPassword(String password) {
        // Implementa la logica di validazione della password
        System.out.println("Password inserita: " + password);
        return password != null && password.length() >= 6 && password.length() <= 100 && !password.contains(" ");
    }

    @FXML
    private void autoLoginIfSessionExists() {
        System.out.println("Tentativo di auto-login...");
        if(SessionUtil.findLastSession() != null) {
            try {
                openHomeScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}