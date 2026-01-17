package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.MessageDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Message;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.ErrorUtil;
import it.camb.fantamaster.util.SessionUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.Cursor;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatViewController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextField inputField;
    @FXML private Button sendButton;

    private League currentLeague;
    private User currentUser;
    private ScheduledExecutorService poller;

    /**
     * Inizializza il controllore della chat con la lega e l'utente corrente.
     * Carica i messaggi e avvia il polling automatico ogni 3 secondi.
     *
     * @param league la lega per cui visualizzare la chat
     */
    public void initData(League league) {
        this.currentLeague = league;
        this.currentUser = SessionUtil.getCurrentSession().getUser();
        
        System.out.println("--- Inizializzazione della chat ---");
        loadMessages();

        startPolling();
    }

    /**
     * Avvia il polling automatico per aggiornare i messaggi della chat.
     * Ricarica i messaggi ogni 3 secondi utilizzando uno ScheduledExecutorService.
     */
    private void startPolling() {
        poller = Executors.newSingleThreadScheduledExecutor();
        poller.scheduleAtFixedRate(() -> {
            loadMessages();
        }, 3, 3, TimeUnit.SECONDS);
    }

    /**
     * Interrompe il polling automatico.
     */
    public void stopPolling() {
        if (poller != null && !poller.isShutdown()) {
            poller.shutdownNow();
            System.out.println("🛑 Polling chat fermato.");
        }
    }

    @FXML
    private void initialize() {
        inputField.textProperty().addListener((obs, oldText, newText) -> {
            sendButton.setDisable(newText.trim().isEmpty());
        });
        sendButton.setDisable(true); // Disabilitato di default
    }

    @FXML
    private void handleSendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        // Disabilita tasto per evitare doppio invio
        sendButton.setDisable(true);

        //Creiamo l'oggetto messaggio
        Message newMessage = new Message(text, currentUser, currentLeague.getId());

        //Salviamo nel DB (in Background)
        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                MessageDAO dao = new MessageDAO(conn);
                return dao.insertMessage(newMessage);
            } catch (SQLException e) {
                ErrorUtil.log("Errore invio messaggio", e);
                return false;
            }
        }).thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    inputField.clear();
                    loadMessages(); // Ricarica la chat per vedere il nuovo messaggio
                } else {
                    System.err.println("Errore invio messaggio");
                }
                sendButton.setDisable(false);
            });
        });
    }

    /**
     * Carica i messaggi della lega dal database e aggiorna l'interfaccia utente.
     * Se la scena non è più visualizzata, interrompe il polling.
     */
    public void loadMessages() {
        if (messageContainer.getScene() == null && poller != null) {
            stopPolling();
            return;
        }
        if (currentLeague == null) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                MessageDAO dao = new MessageDAO(conn);
                return dao.getMessagesByLeagueId(currentLeague.getId());
            } catch (SQLException e) {
                ErrorUtil.log("Errore caricamento messaggi", e);
                return null;
            }
        }).thenAccept(messages -> {
            Platform.runLater(() -> {
                if (messages != null) {
                    updateChatUI(messages);
                }
            });
        });
    }

    private void updateChatUI(List<Message> messages) {
        // Se il numero di messaggi è uguale non ridisegna tutto
        if (messageContainer.getChildren().size() == messages.size()) {
            return; 
        }

        
        boolean wasAtBottom = scrollPane.getVvalue() > 0.9 || messageContainer.getChildren().isEmpty();

        messageContainer.getChildren().clear();

        if (currentUser == null || currentLeague == null) return;
        int myId = currentUser.getId();
        int creatorId = currentLeague.getCreator().getId();
        boolean iamAdmin = (creatorId == myId);

        for (Message msg : messages) {
            boolean isMe = msg.getSender().getId() == currentUser.getId();
            
            HBox row = new HBox(5);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            
            VBox bubble = new VBox(2);
            bubble.setStyle(isMe 
                ? "-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-padding: 8;" 
                : "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 0);"
            );
            bubble.setMaxWidth(250);

            if (!isMe) {
                Label senderName = new Label(msg.getSender().getUsername());
                senderName.setStyle("-fx-font-weight: bold; -fx-font-size: 10; -fx-text-fill: #555;");
                bubble.getChildren().add(senderName);
            }

            Text textNode = new Text(msg.getText());
            TextFlow textFlow = new TextFlow(textNode);
            bubble.getChildren().add(textFlow);

            Label timeLabel = new Label(msg.getFormattedTime());
            timeLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #888;");
            timeLabel.setAlignment(Pos.BOTTOM_RIGHT);
            bubble.getChildren().add(timeLabel);

            //Logica Tasto Delete 
            Node deleteNode = null;
            if (iamAdmin) {
                
                javafx.event.EventHandler<javafx.scene.input.MouseEvent> deleteAction = e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi eliminare?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(res -> { if (res == ButtonType.YES) deleteMessage(msg.getId()); });
                };
                try {
                   String iconPath = "/icons/delete-icon.png";
                   InputStream imgStream = getClass().getResourceAsStream(iconPath);
                   if (imgStream != null) {
                       ImageView deleteIcon = new ImageView(new Image(imgStream));
                       deleteIcon.setFitWidth(20); deleteIcon.setFitHeight(20); deleteIcon.setPreserveRatio(true);
                       Button btnWrapper = new Button();
                       btnWrapper.setGraphic(deleteIcon);
                       btnWrapper.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6;");
                       btnWrapper.setOnMouseEntered(e -> btnWrapper.setStyle("-fx-background-color: rgba(255,0,0,0.1); -fx-cursor: hand; -fx-padding: 4 6 4 6; -fx-background-radius: 5;"));
                       btnWrapper.setOnMouseExited(e -> btnWrapper.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6;"));
                       btnWrapper.setOnMouseClicked(deleteAction);
                       deleteNode = btnWrapper;
                   }
                } catch (Exception e) {}
                if (deleteNode == null) {
                    Label x = new Label("❌"); x.setOnMouseClicked(deleteAction); deleteNode = x;
                }
            }

            if (isMe) {
                if (deleteNode != null) row.getChildren().add(deleteNode);
                row.getChildren().add(bubble);
            } else {
                row.getChildren().add(bubble);
                if (deleteNode != null) row.getChildren().add(deleteNode);
            }

            messageContainer.getChildren().add(row);
        }
        
        // Applica lo scroll in fondo se l'utente era già in fondo
        if (wasAtBottom) {
            messageContainer.applyCss();
            messageContainer.layout();
            scrollPane.layout();
            scrollPane.setVvalue(1.0); // Vai in fondo
        }
    }

    private void deleteMessage(int messageId) {
        System.out.println("🗑️ Tentativo eliminazione messaggio ID: " + messageId);
        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                MessageDAO dao = new MessageDAO(conn);
                return dao.deleteMessage(messageId);
            } catch (SQLException e) {
                ErrorUtil.log("Errore eliminazione messaggio", e);
                return false;
            }
        }).thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    System.out.println("✅ Messaggio eliminato.");
                    loadMessages(); 
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Errore durante l'eliminazione.");
                    error.show();
                }
            });
        });
    }
}