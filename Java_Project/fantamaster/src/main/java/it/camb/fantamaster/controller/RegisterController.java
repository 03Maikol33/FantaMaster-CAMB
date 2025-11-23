package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import it.camb.fantamaster.util.PasswordUtil;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void hideMessageLabel() {
        messageLabel.setText("");
        messageLabel.visibleProperty().set(false);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Compila tutti i campi!");
            messageLabel.visibleProperty().set(true);
            return;
        }

        if (checkUserExists(email)) {
            messageLabel.setText("Esiste già un account con questo indirizzo email!");
            messageLabel.visibleProperty().set(true);
            return;
        }

        if (!checkEmail(email)) {
            messageLabel.setText("Email non valida!");
            messageLabel.visibleProperty().set(true);
            return;
        }
        if (!checkPasswordStrength(password)) {
            messageLabel.setText("La password deve essere lunga almeno 8 caratteri, contenere una lettera maiuscola, una minuscola, un numero e un carattere speciale, e non deve contenere spazi.");
            messageLabel.visibleProperty().set(true);
            return;
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            UserDAO dao = new UserDAO(conn);

            // Hash della password
            String hashedPassword = PasswordUtil.hashPassword(password);

            User nuovoUser = new User();
            nuovoUser.setUsername(username);
            nuovoUser.setEmail(email);
            nuovoUser.setHashPassword(hashedPassword);

            dao.insert(nuovoUser);

            messageLabel.setText("Registrazione completata!");
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.visibleProperty().set(true);
            handleSuccessfulRegister();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Errore nella registrazione!");
            messageLabel.visibleProperty().set(true);
        }
    }

    @FXML
    private void handleSuccessfulRegister() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/successfulRegister.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = (javafx.stage.Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        // Torna alla schermata di login
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = (javafx.stage.Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    private boolean checkPasswordStrength(String password) {
        if (password.length() < 8 || password.contains(" ") || password.length() > 100) return false;
        if (!password.matches(".*[A-Z].*")) return false; // Almeno una lettera maiuscola
        if (!password.matches(".*[a-z].*")) return false; // Almeno una lettera minuscola
        if (!password.matches(".*\\d.*")) return false; // Almeno un numero
        if (!password.matches(".*[!@#$%^&*()].*")) return false; // Almeno un carattere speciale
        return true;
    }

    private boolean checkUserExists(String email) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            UserDAO dao = new UserDAO(conn);
            User user = dao.findByEmail(email);
            return user != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
