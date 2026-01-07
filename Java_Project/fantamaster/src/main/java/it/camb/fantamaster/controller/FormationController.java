package it.camb.fantamaster.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import it.camb.fantamaster.dao.PlayerDAO;
import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.util.ConnectionFactory;
import it.camb.fantamaster.util.SessionUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class FormationController {

    @FXML private Label leagueNameLabel, errorLabel, countLabel, saveFeedbackLabel;
    @FXML private ComboBox<String> moduleComboBox;
    @FXML private AnchorPane pitchPane;
    @FXML private HBox benchContainer;       
    @FXML private HBox availableContainer;   

    private ObservableList<Player> allRoster = FXCollections.observableArrayList();
    private List<Player> startersList = new ArrayList<>(); 
    private ObservableList<Player> orderedBench = FXCollections.observableArrayList();
    
    private Player selectedCaptain = null;
    private PlayerDAO playerDAO;
    private League currentLeague;

    @FXML
    public void initialize() {
        try {
            this.playerDAO = new PlayerDAO(ConnectionFactory.getConnection());
        } catch (SQLException e) { e.printStackTrace(); }
        
        moduleComboBox.setOnAction(e -> {
            startersList.clear(); orderedBench.clear(); selectedCaptain = null;
            refreshAll();
        });

        setupDropTarget(benchContainer, "BENCH");
        setupDropTarget(availableContainer, "AVAILABLE");
    }

    @FXML private void handleBack() { ((Stage) pitchPane.getScene().getWindow()).close(); }

    public void setLeague(League league) {
        this.currentLeague = league;
        leagueNameLabel.setText(league.getName());
        List<String> allowed = league.getAllowedFormationsList();
        moduleComboBox.getItems().setAll(allowed.isEmpty() ? List.of("4-4-2", "4-3-3", "3-5-2") : allowed);
        moduleComboBox.getSelectionModel().selectFirst();
        loadData();
    }

    private void loadData() {
        allRoster.setAll(playerDAO.getTeamRosterByLeague(currentLeague.getName()));
        refreshAll();
    }

    private void refreshAll() {
        refreshPitch();
        refreshBenchUI();
        refreshAvailableUI();
        updateUI();
    }

    private void refreshPitch() {
        pitchPane.getChildren().removeIf(n -> n instanceof StackPane);
        String[] parts = moduleComboBox.getValue().split("-");
        int d = Integer.parseInt(parts[0]), m = Integer.parseInt(parts[1]), a = Integer.parseInt(parts[2]);

        drawRow(Collections.singletonList("P"), 0.88, 1);
        drawRow(Collections.nCopies(d, "D"), 0.65, d);
        drawRow(Collections.nCopies(m, "C"), 0.40, m);
        drawRow(Collections.nCopies(a, "A"), 0.15, a);
    }

    private void drawRow(List<String> roles, double yF, int count) {
        double W = 290, H = 240, spacing = W / (count + 1);
        String roleType = roles.get(0);
        
        // Filtriamo i titolari che appartengono a questo ruolo per distribuirli negli slot
        List<Player> roleStarters = startersList.stream()
                .filter(p -> p.getRuolo().equalsIgnoreCase(roleType))
                .collect(Collectors.toList());

        for (int i = 0; i < count; i++) {
            Player p = (i < roleStarters.size()) ? roleStarters.get(i) : null;
            Node token = createPlayerToken(p, roleType, "FIELD");
            token.setLayoutX((spacing * (i + 1)) - 17); 
            token.setLayoutY((H * yF) - 20);
            pitchPane.getChildren().add(token);
        }
    }

    private void refreshBenchUI() {
        benchContainer.getChildren().clear();
        int rank = 1;
        for (Player p : orderedBench) {
            VBox box = new VBox(2); box.setAlignment(Pos.CENTER);
            Label l = new Label(rank++ + "°"); l.setTextFill(Color.web("#48bb78")); l.setFont(Font.font(8));
            box.getChildren().addAll(l, createPlayerToken(p, p.getRuolo(), "BENCH"));
            benchContainer.getChildren().add(box);
        }
    }

    private void refreshAvailableUI() {
        availableContainer.getChildren().clear();
        for (Player p : allRoster) {
            if (!startersList.contains(p) && !orderedBench.contains(p)) {
                availableContainer.getChildren().add(createPlayerToken(p, p.getRuolo(), "AVAILABLE"));
            }
        }
    }

    private Node createPlayerToken(Player p, String role, String context) {
        StackPane stack = new StackPane(); stack.setPrefSize(34, 42);
        Polygon shirt = new Polygon(0,6, 6,12, 6,34, 28,34, 28,12, 34,6, 28,0, 6,0, 0,6);
        shirt.setStroke(Color.WHITE); shirt.setStrokeWidth(1);
        
        if (p != null) {
            shirt.setFill(switch(p.getRuolo().toUpperCase()){ case "P"->Color.web("#E53E3E"); case "D"->Color.web("#3182CE"); case "C"->Color.web("#D69E2E"); default->Color.web("#38A169"); });
            Label name = new Label(p.getCognome()); name.setTextFill(Color.WHITE); name.setFont(Font.font("System", FontWeight.BOLD, 7));
            stack.getChildren().addAll(shirt, name);
            
            if (context.equals("FIELD") && p.equals(selectedCaptain)) {
                Circle dot = new Circle(4, Color.YELLOW); dot.setStroke(Color.BLACK); dot.setStrokeWidth(0.5);
                stack.getChildren().add(dot); StackPane.setAlignment(dot, Pos.TOP_RIGHT);
            }
            if (context.equals("FIELD")) {
                ContextMenu cm = new ContextMenu(); MenuItem mi = new MenuItem("Imposta Capitano");
                mi.setOnAction(e -> { selectedCaptain = p; refreshAll(); });
                cm.getItems().add(mi); stack.setOnContextMenuRequested(e -> cm.show(stack, e.getScreenX(), e.getScreenY()));
            }
        } else {
            shirt.setFill(Color.rgb(255, 255, 255, 0.05)); shirt.getStrokeDashArray().addAll(3d);
            Label h = new Label(role); h.setTextFill(Color.WHITE); h.setOpacity(0.5); h.setFont(Font.font(8));
            stack.getChildren().addAll(shirt, h);
        }
        setupDrag(stack, p, context, role);
        return stack;
    }

    private void setupDrag(Node node, Player p, String context, String roleReq) {
        node.setOnDragDetected(e -> { if (p != null) { Dragboard db = node.startDragAndDrop(TransferMode.MOVE); ClipboardContent c = new ClipboardContent(); c.putString(p.getId() + ""); db.setContent(c); } });
        node.setOnDragOver(e -> e.acceptTransferModes(TransferMode.MOVE));
        node.setOnDragDropped(e -> {
            int id = Integer.parseInt(e.getDragboard().getString());
            Player dragged = allRoster.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
            if (dragged != null && context.equals("FIELD")) {
                if (!dragged.getRuolo().equalsIgnoreCase(roleReq)) {
                    showWarning("Ruolo sbagliato! Qui serve un " + roleReq); return;
                }
                startersList.remove(dragged); orderedBench.remove(dragged);
                // Controlliamo il limite per ruolo
                long attuali = startersList.stream().filter(x -> x.getRuolo().equalsIgnoreCase(roleReq)).count();
                int max = switch(roleReq.toUpperCase()){ case "P"->1; case "D"->Integer.parseInt(moduleComboBox.getValue().split("-")[0]); case "C"->Integer.parseInt(moduleComboBox.getValue().split("-")[1]); default->Integer.parseInt(moduleComboBox.getValue().split("-")[2]); };
                
                if (attuali >= max) { startersList.stream().filter(x -> x.getRuolo().equalsIgnoreCase(roleReq)).findFirst().ifPresent(startersList::remove); }
                startersList.add(dragged); refreshAll();
            }
        });
    }

    private void setupDropTarget(HBox target, String type) {
        target.setOnDragOver(e -> e.acceptTransferModes(TransferMode.MOVE));
        target.setOnDragDropped(e -> {
            int id = Integer.parseInt(e.getDragboard().getString());
            Player p = allRoster.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
            if (p != null) { startersList.remove(p); orderedBench.remove(p); if (type.equals("BENCH")) orderedBench.add(p); refreshAll(); }
        });
    }

    private void updateUI() {
        countLabel.setText(startersList.size() + "/11");
        countLabel.setTextFill(startersList.size() == 11 ? Color.web("#48bb78") : Color.web("#feb2b2"));
    }

    private void showWarning(String msg) {
        errorLabel.setText(msg); errorLabel.setVisible(true);
        new Timer().schedule(new TimerTask() { @Override public void run() { javafx.application.Platform.runLater(() -> errorLabel.setVisible(false)); } }, 3000);
    }

    @FXML 
    private void handleSaveFormation() {
        // 1. Validazione preliminare: check 11 giocatori e capitano
        if (startersList.size() != 11 || selectedCaptain == null) {
            saveFeedbackLabel.setText("Mancano 11 titolari o il capitano!");
            saveFeedbackLabel.setTextFill(Color.web("#feb2b2")); 
            return;
        }

        try {
            // 2. Chiamata al DAO (che ora restituisce errori parlanti)
            boolean successo = playerDAO.saveFormation(
                SessionUtil.getCurrentSession().getUser().getId(), 
                currentLeague.getId(), 
                moduleComboBox.getValue(), 
                selectedCaptain, 
                startersList, 
                orderedBench
            );

            // 3. Gestione Successo
            if (successo) {
                saveFeedbackLabel.setText("Formazione salvata correttamente!");
                saveFeedbackLabel.setTextFill(Color.web("#48bb78"));
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Successo");
                alert.setHeaderText("Formazione Inviata!");
                alert.setContentText("La tua formazione per la prossima giornata è stata registrata.");
                alert.showAndWait();
            }

        } catch (SQLException e) {
            // 4. Gestione Errore Specifico (Giornata mancante, etc.)
            // Il messaggio viene direttamente dal "throw new SQLException" che abbiamo messo nel DAO
            String errorMessage = e.getMessage();
            
            saveFeedbackLabel.setText("Errore: " + errorMessage);
            saveFeedbackLabel.setTextFill(Color.web("#feb2b2"));

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore Schieramento");
            alert.setHeaderText("Impossibile salvare la formazione");
            alert.setContentText(errorMessage); 
            alert.showAndWait();
            
            // Log per il debug in console
            System.err.println("[Formation] Errore durante il salvataggio: " + e.getErrorCode() + " - " + errorMessage);
        }
    }
}