package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;

public class LeagueAdminScreenController {
    @FXML private StackPane contentArea;
    private League currentLeague; //tiene il riferimento alla lega attualmente aperta

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        System.out.println("Lega corrente impostata in LeagueAdminScreenController: " + league);
    }

    @FXML
    private void showRichieste() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/requestList.fxml"));
            Parent view = loader.load();

            // Recupero il controller della schermata richieste
            RequestListController controller = loader.getController();
            controller.setCurrentLeague(currentLeague); // passo la lega corrente

            // Mostro la view dentro contentArea
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showListone() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/listone.fxml"));
            Parent view = loader.load();

            // Mostro la view dentro contentArea
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void showImpostazioniLega() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leagueAdminSettings.fxml"));
            Parent view = loader.load();

            // Recupero il controller della schermata richieste
            LeagueAdminSettingsController controller = loader.getController();
            controller.setCurrentLeague(currentLeague); // passo la lega corrente

            // Mostro la view dentro contentArea
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackToLeagueList() {
        try{
            Main.showHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 


    // Condividi codice lega basss
    @FXML
private void handleShareLeague() {
    if (currentLeague == null || currentLeague.getInviteCode() == null) {
        System.out.println("Codice non disponibile");
        return;
    }

    String code = currentLeague.getInviteCode();

    // Mostra il codice in un popup
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Condividi Lega");
    alert.setHeaderText("Fai entrare i tuoi amici!");
    alert.setContentText("Il codice della tua lega è: " + code + "\n\n(Il codice è stato copiato negli appunti)");

    // Copia automatica negli appunti (comodissimo per l'utente)
    Clipboard clipboard = Clipboard.getSystemClipboard();
    ClipboardContent content = new ClipboardContent();
    content.putString(code);
    clipboard.setContent(content);

    alert.showAndWait();
} 
}
// Fix conflitti definitivo
