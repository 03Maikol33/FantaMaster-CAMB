package it.camb.fantamaster.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import it.camb.fantamaster.dao.*;
import it.camb.fantamaster.model.*;
import it.camb.fantamaster.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class TradesController {

    @FXML private ComboBox<Player> myPlayersCombo;
    @FXML private ComboBox<User> opponentsCombo;
    @FXML private ComboBox<Player> opponentPlayersCombo;
    @FXML private Label marketStatusLabel;
    @FXML private VBox proposalForm;
    @FXML private VBox receivedTradesContainer;

    private League currentLeague;
    private User currentUser;
    private Rosa miaRosa;
    private PlayerDAO playerDAO;

    @FXML
    public void initialize() {
        opponentsCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return (u == null) ? "" : u.getUsername(); }
            @Override public User fromString(String s) { return null; }
        });

        StringConverter<Player> pConv = new StringConverter<Player>() {
            @Override public String toString(Player p) { 
                return (p == null) ? "" : p.getNome() + " " + p.getCognome() + " (" + p.getRuolo() + ")"; 
            }
            @Override public Player fromString(String s) { return null; }
        };
        myPlayersCombo.setConverter(pConv);
        opponentPlayersCombo.setConverter(pConv);
    }

    public void setLeague(League league) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            this.playerDAO = new PlayerDAO(conn);
            this.currentLeague = new LeagueDAO(conn).getLeagueById(league.getId());
            this.currentUser = SessionUtil.getCurrentSession().getUser();
            
            if (currentLeague.isMercatoAperto()) {
                marketStatusLabel.setText("● MERCATO APERTO");
                marketStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                proposalForm.setDisable(false);
            } else {
                marketStatusLabel.setText("○ MERCATO CHIUSO");
                marketStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                proposalForm.setDisable(true);
            }
            loadInitialData(conn);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadInitialData(Connection conn) throws SQLException {
        this.miaRosa = new RosaDAO(conn).getRosaByUserAndLeague(currentUser.getId(), currentLeague.getId());
        if (miaRosa != null) {
            myPlayersCombo.getItems().setAll(playerDAO.getPlayersByRosa(miaRosa.getId()));
        }
        List<User> users = new UsersLeaguesDAO(conn).getParticipants(currentLeague.getId());
        users.removeIf(u -> u.getId() == currentUser.getId());
        opponentsCombo.getItems().setAll(users);
        loadReceivedTrades(); 
    }

    @FXML
    private void loadOpponentPlayers() {
        User opponent = opponentsCombo.getValue();
        if (opponent == null) return;
        try {
            Connection conn = ConnectionFactory.getConnection();
            Rosa rosaOpp = new RosaDAO(conn).getRosaByUserAndLeague(opponent.getId(), currentLeague.getId());
            if (rosaOpp != null) {
                opponentPlayersCombo.getItems().setAll(playerDAO.getPlayersByRosa(rosaOpp.getId()));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSendProposal() {
        Player mio = myPlayersCombo.getValue();
        Player suo = opponentPlayersCombo.getValue();
        User target = opponentsCombo.getValue();

        if (mio == null || suo == null) {
            showAlert("Errore", "Seleziona entrambi i giocatori!");
            return;
        }

        if (!mio.getRuolo().equalsIgnoreCase(suo.getRuolo())) {
            showAlert("Ruolo Incompatibile", "Puoi scambiare solo giocatori dello STESSO RUOLO!");
            return;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            Rosa rosaRicevente = new RosaDAO(conn).getRosaByUserAndLeague(target.getId(), currentLeague.getId());
            Scambio s = new Scambio();
            s.setLegaId(currentLeague.getId());
            s.setRosaRichiedenteId(miaRosa.getId());
            s.setRosaRiceventeId(rosaRicevente.getId());
            s.setGiocatoreOffertoId(mio.getId()); 
            s.setGiocatoreRichiestoId(suo.getId()); 

            if (new ScambiDAO(conn).proponiScambio(s)) {
                showAlert("Successo", "Trattativa inviata!");
                myPlayersCombo.getSelectionModel().clearSelection();
                opponentPlayersCombo.getSelectionModel().clearSelection();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void loadReceivedTrades() {
        if (miaRosa == null) return;
        receivedTradesContainer.getChildren().clear();
        try {
            Connection conn = ConnectionFactory.getConnection();
            List<Scambio> ricevuti = new ScambiDAO(conn).getScambiRicevuti(miaRosa.getId());
            for (Scambio s : ricevuti) {
                receivedTradesContainer.getChildren().add(createTradeCard(s));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox createTradeCard(Scambio s) {
        VBox card = new VBox(10);
        card.getStyleClass().add("modern-card");
        card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("RICHIESTA DA: " + s.getNomeRichiedente());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e40af;");
        Label desc = new Label("Ti offre: " + s.getNomeGiocatoreOfferto() + "\nIn cambio di: " + s.getNomeGiocatoreRichiesto());
        desc.setStyle("-fx-font-size: 13; -fx-text-fill: #475569;");

        HBox actions = new HBox(12);
        Button ok = new Button("ACCETTA"); ok.getStyleClass().add("success-button");
        ok.setOnAction(e -> { try { Connection c = ConnectionFactory.getConnection(); new ScambiDAO(c).accettaScambio(s); loadReceivedTrades(); } catch (SQLException ex) { ex.printStackTrace(); } });
        Button no = new Button("RIFIUTA"); no.getStyleClass().add("danger-button");
        no.setOnAction(e -> { try { Connection c = ConnectionFactory.getConnection(); new ScambiDAO(c).rifiutaScambio(s.getId()); loadReceivedTrades(); } catch (SQLException ex) { ex.printStackTrace(); } });

        actions.getChildren().addAll(ok, no);
        card.getChildren().addAll(title, desc, actions);
        return card;
    }

    @FXML private void handleBack() { ((Stage) marketStatusLabel.getScene().getWindow()).close(); }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(c);
        a.showAndWait();
    }
}