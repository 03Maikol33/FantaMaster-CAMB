package it.camb.fantamaster.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SuccessfulRegisterController {
    @FXML private Label titleLabel;

    @FXML
    private void handleLogin(){
        try{
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) titleLabel.getScene().getWindow(); // recupera lo stage corrente
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
// Fix conflitti definitivo
