package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.MessageDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Message;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
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
    private ScheduledExecutorService poller; // Il timer per l'aggiornamento

    // Metodo chiamato da chi apre la chat per passare i dati
    public void initData(League league) {
        this.currentLeague = league;
        this.currentUser = SessionUtil.getCurrentSession().getUser();
        
        System.out.println("--- INIT CHAT ---");
        // ... (tuoi log esistenti) ...

        // 1. Carico subito i messaggi
        loadMessages();

        // 2. AVVIO IL POLLING AUTOMATICO (Ogni 3 secondi)
        startPolling();
    }

    private void startPolling() {
        // Crea un timer con un solo thread
        poller = Executors.newSingleThreadScheduledExecutor();
        
        // Esegui loadMessages() ogni 3 secondi
        // param 1: azione, param 2: ritardo iniziale, param 3: ogni quanto tempo, param 4: unità
        poller.scheduleAtFixedRate(() -> {
            // Nota: loadMessages gestisce già il Platform.runLater internamente, 
            // quindi possiamo chiamarlo direttamente dal thread del timer.
            loadMessages();
        }, 3, 3, TimeUnit.SECONDS);
    }

    /**
     * IMPORTANTE: Questo metodo deve essere chiamato quando si esce dalla chat!
     * Altrimenti il timer continua a girare all'infinito e consuma risorse/connessioni.
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

        // 1. Creiamo l'oggetto messaggio
        Message newMessage = new Message(text, currentUser, currentLeague.getId());

        // 2. Salviamo nel DB (in Background)
        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                MessageDAO dao = new MessageDAO(conn);
                return dao.insertMessage(newMessage);
            } catch (SQLException e) {
                e.printStackTrace();
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

    public void loadMessages() {
        // SAFEGUARD: Se la scena non esiste più o il poller deve fermarsi
        if (messageContainer.getScene() == null && poller != null) {
            // Significa che questa schermata non è più visualizzata a video
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
                e.printStackTrace();
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
        // OTTIMIZZAZIONE 1: Se il numero di messaggi è uguale, non ridisegnare tutto!
        // Questo evita sfarfallii inutili ogni 3 secondi se nessuno ha scritto.
        if (messageContainer.getChildren().size() == messages.size()) {
            return; 
        }

        // SMART SCROLL 1: Capire se l'utente è in fondo PRIMA di cancellare tutto.
        // Se vValue è > 0.9, significa che l'utente è nell'ultimo 10% della chat (quindi in basso).
        // Se children è vuoto, è il primo caricamento, quindi dobbiamo scorrere giù.
        boolean wasAtBottom = scrollPane.getVvalue() > 0.9 || messageContainer.getChildren().isEmpty();

        messageContainer.getChildren().clear();

        // --- DEBUG LOGS (Rimossi per pulizia, ma utili se serve) ---
        if (currentUser == null || currentLeague == null) return;
        int myId = currentUser.getId();
        int creatorId = currentLeague.getCreator().getId();
        boolean iamAdmin = (creatorId == myId);
        // ------------------

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

            // --- Logica Tasto Delete (Copiata dalla tua versione precedente) ---
            Node deleteNode = null;
            if (iamAdmin) {
                // ... (tutto il codice del bottone delete rimane identico a prima) ...
                // Per brevità non lo riscrivo tutto qui, ma tu MANTIENILO UGUALE!
                // Usa il codice "Button Wrapper" che ti ho dato nell'ultimo messaggio.
                
                // --- INIZIO BLOCCO DELETE (Ri-incolla qui il codice del bottone wrapper) ---
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
                // --- FINE BLOCCO DELETE ---
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
        
        // SMART SCROLL 2: Applicare lo scroll solo se necessario
        if (wasAtBottom) {
            // Un piccolo trucco: layout() forza il calcolo delle altezze PRIMA di scrollare
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
                e.printStackTrace();
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