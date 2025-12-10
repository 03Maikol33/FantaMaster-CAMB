package it.camb.fantamaster.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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

    @FXML
    private void initialize() {
        // Platform.runLater assicura che questo codice giri dopo che la UI è pronta
        Platform.runLater(this::autoLoginIfSessionExists);
    }

    /**
     * Gestisce il click sul bottone Login.
     * Si occupa SOLO del flusso UI: Validazione -> Autenticazione -> Navigazione o Errore.
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // 1. Validazione Input (Client Side)
        if (!isValidInput(email, password)) {
            showError("Email o Password non valide.");
            return;
        }

        try {
            // 2. Logica di Business (Server Side)
            User user = authenticateUser(email, password);
            
            // 3. Successo: Creazione Sessione e Navigazione
            System.out.println("Login effettuato con successo: " + user.getUsername());
            SessionUtil.createSession(user);
            openHomeScreen();

        } catch (SecurityException e) {
            // Errore credenziali (Utente non trovato o Password errata)
            showError(e.getMessage());
        } catch (SQLException e) {
            // Errore tecnico (Database down, timeout, ecc.)
            e.printStackTrace();
            showError("Errore di connessione al server. Riprova.");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore nel caricamento della Home Page.");
        }
    }

    /**
     * Verifica le credenziali nel Database.
     * NON tocca la UI. Lancia eccezioni se qualcosa non va.
     * * @return User l'oggetto utente se autenticato.
     * @throws SQLException Errore DB.
     * @throws SecurityException Se le credenziali sono errate.
     */
    private User authenticateUser(String email, String password) throws SQLException, SecurityException {
        // NOTA: Non usiamo try-with-resources perché usiamo il Singleton ConnectionFactory 
        // che mantiene la connessione viva per evitare timeout/limiti cloud.
        Connection conn = ConnectionFactory.getConnection();
        
        UserDAO userDAO = new UserDAO(conn);
        User user = userDAO.findByEmail(email);

        if (user == null) {
            throw new SecurityException("Utente non trovato. Registrati.");
        }

        if (!PasswordUtil.checkPassword(password, user.getHashPassword())) {
            throw new SecurityException("Password errata.");
        }

        return user;
    }

    @FXML
    private void handleRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossibile aprire la registrazione.");
        }
    }

    private void openHomeScreen() throws IOException {
        Main.showHome();
    }

    private void autoLoginIfSessionExists() {
        if (SessionUtil.findLastSession() != null) {
            System.out.println("Sessione trovata, auto-login in corso...");
            try {
                openHomeScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Metodi Helper e Validatori ---

    @FXML
    private void hideErrorLabel() {
        errorLabel.setVisible(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private boolean isValidInput(String email, String password) {
        // Validazione sintattica semplice
        boolean validEmail = email != null && email.contains("@") && email.length() > 3;
        boolean validPass = password != null && !password.isEmpty();
        return validEmail && validPass;
    }
}
// Fix conflitti definitivo