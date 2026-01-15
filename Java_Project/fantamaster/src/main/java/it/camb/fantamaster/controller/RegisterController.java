package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.regex.Pattern;

import it.camb.fantamaster.dao.UserDAO;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void hideMessageLabel() {
        messageLabel.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // 1. Validazione Input (Client Side)
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Compila tutti i campi!");
            return;
        }

        if (!checkEmail(email)) {
            showError("Email non valida!");
            return;
        }

        if (!checkPasswordStrength(password)) {
            showError("La password non rispetta i criteri di sicurezza (Min 8 car, Maiusc, Minusc, Numero, Speciale).");
            return;
        }

        // 2. Interazione con Database (Server Side)
        try {
            // NOTA: Connessione aperta ma NON chiusa (Singleton Safe)
            Connection conn = ConnectionFactory.getConnection();
            UserDAO dao = new UserDAO(conn);

            // CHECK UNIVOCITÀ: Solo sull'Email
            if (dao.findByEmail(email) != null) {
                showError("Esiste già un account con questa email!");
                return;
            }

            // 3. Creazione Utente
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            User nuovoUser = new User();
            nuovoUser.setUsername(username);
            nuovoUser.setEmail(email);
            nuovoUser.setHashPassword(hashedPassword);
            // created_at gestito dal DB o qui se preferisci

            // 4. Inserimento
            if (dao.insert(nuovoUser)) {
                // Successo
                messageLabel.setText("Registrazione completata!");
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setVisible(true);
                handleSuccessfulRegister();
            } else {
                showError("Errore sconosciuto durante il salvataggio.");
            }

        } catch (SQLException e) {
            ErrorUtil.log("Errore di connessione al Database durante la registrazione", e);
            showError("Errore di connessione al Database.");
        } catch (Exception e) {
            ErrorUtil.log("Errore imprevisto durante la registrazione", e);
            showError("Errore imprevisto.");
        }
    }

    // --- Navigazione ---

    @FXML
    private void handleSuccessfulRegister() {
        navigateTo("/fxml/successfulRegister.fxml");
    }

    @FXML
    private void handleLogin() {
        navigateTo("/fxml/login.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            ErrorUtil.log("Impossibile caricare la schermata successiva", e);
            showError("Impossibile caricare la schermata successiva.");
        }
    }

    // --- Helper e Validatori ---

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setVisible(true);
    }

    private boolean checkEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean checkPasswordStrength(String password) {
        if (password.length() < 8 || password.length() > 100 || password.contains(" ")) return false;

        // Definiamo i pattern una volta sola (meglio se come costanti statiche della classe)
        boolean hasUpper   = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLower   = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasDigit   = Pattern.compile("\\d").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[!@#$%^&*()]").matcher(password).find();

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
// Fix conflitti definitivo