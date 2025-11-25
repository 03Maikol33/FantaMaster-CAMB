package it.camb.fantamaster.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;

import it.camb.fantamaster.dao.RequestDAO;
import it.camb.fantamaster.model.Request;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
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

    // Chiamare questo metodo dal codice che carica la cella nella ListView
    // requester: utente che ha inviato la richiesta
    // timestamp: stringa già formattata della data/ora della richiesta (es. "2025-11-25 12:34")
    // avatar: immagine in byte[] opzionale (può essere null)
    public void setRequestData(Request request) {
        User requester = request.getUser();
        String timestamp = request.getTimestamp() != null ? request.getTimestamp().toString().replace('T', ' ') : null;
        byte[] avatar = requester.getAvatar();
        if (requester == null) {
            throw new IllegalArgumentException("Requester nullo");
        }

        userName.setText(requester.getUsername());
        requestTimestamp.setText(timestamp != null ? timestamp : "");

        if (avatar != null) {
            userIcon.setImage(new Image(new ByteArrayInputStream(avatar)));
        } else {
            System.out.println("Avatar non presente per l'utente: "+requester);
            // immagine di default per l'utente (assicurati che il file esista nel classpath)
            InputStream stream = getClass().getResourceAsStream("/images/userDefaultPic.png");
            if (stream != null) {
                userIcon.setImage(new Image(stream));
            } else {
                System.err.println("Stream nullo: immagine non trovata nel classpath!");
            }

            //userIcon.setImage(new Image(getClass().getResourceAsStream("/images/userDefaultPic.png")));
        }

        //se la legha ha le iscrizioni chiuse oppure è piena, disabilita il pulsante di approvazione
        if(request.getLeague().isRegistrationsClosed() || request.getLeague().getParticipants().size() >= request.getLeague().getMaxMembers()) {
            approveLabel.setDisable(true);
            approveLabel.getStyleClass().add("disabled-label");
        }else{
            approveLabel.setDisable(false);
            approveLabel.getStyleClass().remove("disabled-label");
        }

        // rendi cliccabili i label di approva/rifiuta
        approveLabel.setOnMouseClicked(e -> handleApprove());
        rejectLabel.setOnMouseClicked(e -> handleReject());
    }

    @FXML
    private void handleApprove(){
        if(request.getLeague().isRegistrationsClosed()) {
            System.out.println("Impossibile approvare la richiesta: le iscrizioni sono chiuse per questa lega.");
            return;
        }
        try{
            Connection conn = ConnectionFactory.getConnection();
            System.out.println("Approvata richiesta di: " + request.getUser());
            RequestDAO requestDAO = new RequestDAO(conn);
            
            requestDAO.approveRequest(request.getUser(), request.getLeague());
            if(parentController != null){
                parentController.refreshList();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @FXML
    private void handleReject() {
        System.out.println("Rifiutata richiesta di: " + request.getUser());
        try{
            Connection conn = ConnectionFactory.getConnection();
            RequestDAO requestDAO = new RequestDAO(conn);

            requestDAO.rejectRequest(request.getUser(), request.getLeague());
            if(parentController != null){
                parentController.refreshList();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void setParentController(RequestListController parentController) {
        this.parentController = parentController;
    }
}
