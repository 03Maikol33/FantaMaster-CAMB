package it.camb.fantamaster.controller;

import it.camb.fantamaster.Main;
import it.camb.fantamaster.model.League;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;

public class LeagueAdminScreenController {

    @FXML 
    private StackPane contentArea;
    @FXML
    private Button auctionButton;
    
    private League currentLeague;

    public void setCurrentLeague(League league) {
        this.currentLeague = league;
        System.out.println("Lega corrente impostata: " + (league != null ? league.getName() : "Null"));
        if(league.isAuctionOpen()) {
            auctionButton.setDisable(false);
        } else {
            auctionButton.setDisable(true);
        }
    }

    // --- METODI DI NAVIGAZIONE ---

    @FXML
    private void showRichieste() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/requestList.fxml"));
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof RequestListController) {
                ((RequestListController) controller).setCurrentLeague(currentLeague);
            }

            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Errore caricamento Richieste", e.getMessage());
        }
    }

    @FXML
    private void openChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatView.fxml"));
            Parent view = loader.load();

            ChatViewController controller = loader.getController();
            controller.initData(currentLeague); 

            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Errore caricamento Chat", e.getMessage());
        }
    }

    @FXML
    private void openAuction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionMainContainer.fxml"));
            Parent view = loader.load();
            
            AuctionMainContainerController controller = loader.getController();
            controller.initData(currentLeague.getId()); 

            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Errore caricamento Asta", e.getMessage());
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

            LeagueAdminSettingsController controller = loader.getController();
            controller.setCurrentLeague(currentLeague);
            controller.setParentController(this);

            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Errore caricamento Impostazioni", e.getMessage());
        }
    }

    // --- METODI TOP BAR ---

    @FXML
    private void goBackToLeagueList() {
        try {
            Main.showHome(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShareLeague() {
        if (currentLeague == null || currentLeague.getInviteCode() == null) {
            System.out.println("Codice non disponibile");
            return;
        }

        String code = currentLeague.getInviteCode();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Condividi Lega");
        alert.setHeaderText("Fai entrare i tuoi amici!");
        alert.setContentText("Il codice della tua lega è: " + code + "\n\n(Il codice è stato copiato negli appunti)");

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(code);
        clipboard.setContent(content);

        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}