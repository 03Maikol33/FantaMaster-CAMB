package it.camb.fantamaster.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class RequestListItemController {

    @FXML private ImageView userIcon;
    @FXML private Label userName;
    @FXML private Label requestTimestamp;
    @FXML private Label approveLabel;
    @FXML private Label rejectLabel;

    private Request request;
    private RequestListController parentController;

    // Formatter per la data (più pulito del replace)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Imposta la richiesta e aggiorna immediatamente la grafica.
     */
    public void setRequest(Request request) {
        this.request = request;
        updateUI();
    }

    public void setParentController(RequestListController parentController) {
        this.parentController = parentController;
    }

    private void updateUI() {
        if (request == null) return;

        User requester = request.getUser();
        
        // 1. Gestione Utente (Null Safe)
        if (requester != null) {
            userName.setText(requester.getUsername());
            
            // Gestione Avatar
            byte[] avatar = requester.getAvatar();
            if (avatar != null && avatar.length > 0) {
                userIcon.setImage(new Image(new ByteArrayInputStream(avatar)));
            } else {
                setDefaultImage();
            }
        } else {
            userName.setText("Utente Sconosciuto");
            setDefaultImage();
        }

        // 2. Gestione Timestamp
        if (request.getTimestamp() != null) {
            requestTimestamp.setText(request.getTimestamp().format(DATE_FORMATTER));
        } else {
            requestTimestamp.setText("");
        }

        // 3. Logica UI: Disabilita approvazione se lega piena o chiusa
        boolean isFull = request.getLeague().getParticipants().size() >= request.getLeague().getMaxMembers();
        boolean isClosed = request.getLeague().isRegistrationsClosed();

        if (isClosed || isFull) {
            approveLabel.setDisable(true);
            // Assicurati di avere la classe CSS o rimuovi questa riga se usi l'opacità standard di JavaFX
            approveLabel.getStyleClass().add("disabled-label"); 
            
            // Opzionale: Tooltip per spiegare perché è disabilitato
            // Tooltip.install(approveLabel, new Tooltip("Lega piena o iscrizioni chiuse"));
        } else {
            approveLabel.setDisable(false);
            approveLabel.getStyleClass().remove("disabled-label");
        }

        // 4. Event Handlers
        approveLabel.setOnMouseClicked(e -> handleApprove());
        rejectLabel.setOnMouseClicked(e -> handleReject());
    }

    private void setDefaultImage() {
        try {
            InputStream stream = getClass().getResourceAsStream("/images/userDefaultPic.png");
            if (stream != null) {
                userIcon.setImage(new Image(stream));
            }
        } catch (Exception e) {
            System.err.println("Impossibile caricare icona default utente.");
        }
    }

    @FXML
    private void handleApprove() {
        if (request.getLeague().isRegistrationsClosed()) return;

        // 1. FEEDBACK IMMEDIATO: Disabilita per evitare doppi click e mostrare che lavora
        setActionsDisabled(true);
        approveLabel.setText("..."); // Opzionale: mostra che sta caricando

        // 2. BACKGROUND WORK
        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                RequestDAO requestDAO = new RequestDAO(conn);
                System.out.println("Approvo richiesta di: " + request.getUser().getUsername());
                return requestDAO.approveRequest(request.getUser(), request.getLeague());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }).thenAccept(success -> {
            // 3. UI UPDATE
            Platform.runLater(() -> {
                if (success && parentController != null) {
                    parentController.refreshList(); // Ricarica la lista (che farà partire lo spinner del padre)
                } else {
                    // Se fallisce, riabilita i bottoni e mostra errore (o stampa in console)
                    System.err.println("Errore approvazione.");
                    setActionsDisabled(false);
                    approveLabel.setText("Approva");
                }
            });
        });
    }

    @FXML
    private void handleReject() {
        // 1. FEEDBACK IMMEDIATO
        setActionsDisabled(true);
        rejectLabel.setText("...");

        // 2. BACKGROUND WORK
        CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = ConnectionFactory.getConnection();
                RequestDAO requestDAO = new RequestDAO(conn);
                return requestDAO.rejectRequest(request.getUser(), request.getLeague());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }).thenAccept(success -> {
            // 3. UI UPDATE
            Platform.runLater(() -> {
                if (success && parentController != null) {
                    parentController.refreshList();
                } else {
                    setActionsDisabled(false);
                    rejectLabel.setText("Rifiuta");
                }
            });
        });
    }

    private void setActionsDisabled(boolean disabled) {
        approveLabel.setDisable(disabled);
        rejectLabel.setDisable(disabled);
        if (disabled) {
            approveLabel.getStyleClass().add("disabled-label");
            rejectLabel.getStyleClass().add("disabled-label");
        } else {
            approveLabel.getStyleClass().remove("disabled-label");
            rejectLabel.getStyleClass().remove("disabled-label");
        }
    }
}
// Fix conflitti definitivo