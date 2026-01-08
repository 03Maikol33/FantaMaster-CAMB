package it.camb.fantamaster.controller;

import it.camb.fantamaster.dao.RosaDAO;
import it.camb.fantamaster.dao.UsersLeaguesDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Rosa;
import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class LeagueDashboardController {

    @FXML private ImageView leagueLogoImageView;
    @FXML private Label leagueNameLabel;
    @FXML private VBox participantsContainer;

    public void initData(League league) {
        leagueNameLabel.setText(league.getName());
        
        try (Connection conn = ConnectionFactory.getConnection()) {
            UsersLeaguesDAO userLeagueDao = new UsersLeaguesDAO(conn);
            RosaDAO rosaDao = new RosaDAO(conn); // Istanziamo RosaDAO
            
            List<User> participants = userLeagueDao.getParticipants(league.getId());
            
            participantsContainer.getChildren().clear();
            for (User user : participants) {
                // Recuperiamo la rosa dell'utente per questa specifica lega
                Rosa rosa = rosaDao.getRosaByUserAndLeague(user.getId(), league.getId());
                
                // Se la rosa esiste e ha un nome, lo usiamo, altrimenti mettiamo un placeholder
                String teamName = (rosa != null && rosa.getNomeRosa() != null) ? rosa.getNomeRosa() : "Squadra in attesa";
                
                participantsContainer.getChildren().add(createParticipantRow(user, teamName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createParticipantRow(User user, String teamName) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("modern-card");
        row.setStyle("-fx-padding: 12; -fx-background-color: white;");

        // Avatar Circolare
        ImageView avatarView = new ImageView();
        avatarView.setFitHeight(45);
        avatarView.setFitWidth(45);
        
        if (user.getAvatar() != null) {
            avatarView.setImage(new Image(new ByteArrayInputStream(user.getAvatar())));
        } else {
            avatarView.setImage(new Image(getClass().getResourceAsStream("/images/userDefaultPic.png")));
        }
        
        Circle clip = new Circle(22.5, 22.5, 22.5);
        avatarView.setClip(clip);

        // Testi
        VBox texts = new VBox(2);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        
        // Ora visualizza correttamente il NOME DELLA ROSA
        Label teamLabel = new Label(teamName);
        teamLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-text-transform: uppercase; -fx-letter-spacing: 1;");
        
        texts.getChildren().addAll(nameLabel, teamLabel);
        row.getChildren().addAll(avatarView, texts);

        return row;
    }
}