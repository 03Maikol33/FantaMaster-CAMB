package it.camb.fantamaster.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import it.camb.fantamaster.dao.PlayerDAO;
import it.camb.fantamaster.model.Player;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;
import javafx.scene.layout.VBox;

public class ListoneController {
    @FXML private VBox playerContainer;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private Button resetFilterButton;

    private final PlayerDAO playerDAO = new PlayerDAO();
    private final int pageSize = 100; // elementi per pagina (aumentato)
    private int offset = 0;
    public VBox getPlayerContainer() {
        return playerContainer;
    }

    public void setPlayerContainer(VBox playerContainer) {
        this.playerContainer = playerContainer;
    }

    public ComboBox<String> getRoleCombo() {
        return roleCombo;
    }

    public void setRoleCombo(ComboBox<String> roleCombo) {
        this.roleCombo = roleCombo;
    }

    public TextField getMinPriceField() {
        return minPriceField;
    }

    public void setMinPriceField(TextField minPriceField) {
        this.minPriceField = minPriceField;
    }

    public TextField getMaxPriceField() {
        return maxPriceField;
    }

    public void setMaxPriceField(TextField maxPriceField) {
        this.maxPriceField = maxPriceField;
    }

    public Button getResetFilterButton() {
        return resetFilterButton;
    }

    public void setResetFilterButton(Button resetFilterButton) {
        this.resetFilterButton = resetFilterButton;
    }

    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public Button getLoadMoreButton() {
        return loadMoreButton;
    }

    public void setLoadMoreButton(Button loadMoreButton) {
        this.loadMoreButton = loadMoreButton;
    }

    public String getRoleFilter() {
        return roleFilter;
    }

    public void setRoleFilter(String roleFilter) {
        this.roleFilter = roleFilter;
    }

    public Integer getMinPriceFilter() {
        return minPriceFilter;
    }

    public void setMinPriceFilter(Integer minPriceFilter) {
        this.minPriceFilter = minPriceFilter;
    }

    public Integer getMaxPriceFilter() {
        return maxPriceFilter;
    }

    public void setMaxPriceFilter(Integer maxPriceFilter) {
        this.maxPriceFilter = maxPriceFilter;
    }

    private boolean loading = false;
    private Button loadMoreButton;

    // current filters
    private String roleFilter = null;
    private Integer minPriceFilter = null;
    private Integer maxPriceFilter = null;

    @FXML
    public void initialize() {
        // populate role combo with distinct roles (including empty = All)
        CompletableFuture.runAsync(() -> {
            try {
                List<Player> all = getPlayerDAO().getAllPlayers();
                List<String> roles = new ArrayList<>();
                for (Player p : all) {
                    String r = p.getRuolo();
                    if (!roles.contains(r)) roles.add(r);
                }
                Platform.runLater(() -> {
                    getRoleCombo().getItems().add("Tutti");
                    // Prefer a standard role order: P, D, C, A. Then any others.
                    List<String> preferred = List.of("P", "D", "C", "A");
                    for (String pr : preferred) {
                        if (roles.contains(pr)) {
                            getRoleCombo().getItems().add(pr);
                        }
                    }
                    for (String r : roles) {
                        if (!preferred.contains(r)) getRoleCombo().getItems().add(r);
                    }
                    getRoleCombo().getSelectionModel().selectFirst();

                    // enforce numeric-only input for price fields
                    UnaryOperator<TextFormatter.Change> integerFilter = change -> {
                        String newText = change.getControlNewText();
                        return (newText.matches("\\d*")) ? change : null;
                    };
                    getMinPriceField().setTextFormatter(new TextFormatter<>(integerFilter));
                    getMaxPriceField().setTextFormatter(new TextFormatter<>(integerFilter));

                    // Apply filters when role changes
                    getRoleCombo().setOnAction(e -> applyFilters());

                    // Apply filters when Enter pressed in the price fields
                    getMinPriceField().setOnAction(e -> applyFilters());
                    getMaxPriceField().setOnAction(e -> applyFilters());

                    getResetFilterButton().setOnAction(e -> resetFilters());

                    loadNextPage();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadNextPage());
            }
        });
    }

    private void loadNextPage() {
        if (isLoading()) return;
        setLoading(true);

        // Rimuovo eventuale pulsante "Carica altri" prima di mostrare lo spinner
        if (getLoadMoreButton() != null) getPlayerContainer().getChildren().remove(getLoadMoreButton());

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        VBox spinnerBox = new VBox(spinner);
        spinnerBox.setAlignment(Pos.CENTER);
        spinnerBox.setPrefHeight(120);
        getPlayerContainer().getChildren().add(spinnerBox);

        // choose DAO method depending on filters
        CompletableFuture.supplyAsync(() -> {
            try {
                if ((getRoleFilter() == null || getRoleFilter().equals("Tutti")) && getMinPriceFilter() == null && getMaxPriceFilter() == null) {
                    return getPlayerDAO().getPlayersPage(getOffset(), getPageSize());
                } else {
                    String roleParam = (getRoleFilter() != null && getRoleFilter().equals("Tutti")) ? null : getRoleFilter();
                    return getPlayerDAO().getPlayersPageFiltered(getOffset(), getPageSize(), roleParam, getMinPriceFilter(), getMaxPriceFilter());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return List.<Player>of();
            }
        }).thenAccept(players -> Platform.runLater(() -> {
            // rimuovo lo spinner
            getPlayerContainer().getChildren().remove(spinnerBox);

            if (players.isEmpty() && getOffset() == 0) {
                Label empty = new Label("Nessun giocatore disponibile.");
                getPlayerContainer().getChildren().add(empty);
            } else {
                for (Player player : players) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/playerItem.fxml"));
                        Node item = loader.load();
                        PlayerItemController controller = loader.getController();
                        controller.setPlayer(player);
                        getPlayerContainer().getChildren().add(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                setOffset(getOffset() + players.size());

                // Se abbiamo ancora altri elementi, mostro il pulsante per caricare altri
                int total;
                if ((getRoleFilter() == null || getRoleFilter().equals("Tutti")) && getMinPriceFilter() == null && getMaxPriceFilter() == null) {
                    total = getPlayerDAO().getTotalCount();
                } else {
                    String roleParam = (getRoleFilter() != null && getRoleFilter().equals("Tutti")) ? null : getRoleFilter();
                    total = getPlayerDAO().getFilteredTotalCount(roleParam, getMinPriceFilter(), getMaxPriceFilter());
                }
                if (getOffset() < total) {
                    ensureLoadMoreButton();
                    if (!getPlayerContainer().getChildren().contains(getLoadMoreButton())) {
                        getPlayerContainer().getChildren().add(getLoadMoreButton());
                    }
                } else {
                    // rimosso il bottone se non serve
                    if (getLoadMoreButton() != null) getPlayerContainer().getChildren().remove(getLoadMoreButton());
                }
            }

            setLoading(false);
        }));
    }

    private void ensureLoadMoreButton() {
        if (getLoadMoreButton() != null) return;
        setLoadMoreButton(new Button("Carica altri"));
        getLoadMoreButton().setOnAction(e -> loadNextPage());
        getLoadMoreButton().setStyle("-fx-background-color: transparent; -fx-padding: 10 20;");
    }

    public void applyFilters() {
        String selectedRole = getRoleCombo().getSelectionModel().getSelectedItem();
        setRoleFilter((selectedRole != null && !selectedRole.equals("Tutti")) ? selectedRole : "Tutti");

        try {
            String minText = getMinPriceField().getText();
            String maxText = getMaxPriceField().getText();
            setMinPriceFilter((minText == null || minText.isBlank()) ? null : Integer.valueOf(minText.trim()));
            setMaxPriceFilter((maxText == null || maxText.isBlank()) ? null : Integer.valueOf(maxText.trim()));
        } catch (NumberFormatException ex) {
            // invalid number -> ignore and set null
            setMinPriceFilter(null);
            setMaxPriceFilter(null);
        }

        // reset pagination and UI
        setOffset(0);
        getPlayerContainer().getChildren().clear();
        loadNextPage();
    }

    private void resetFilters() {
        getRoleCombo().getSelectionModel().selectFirst();
        getMinPriceField().clear();
        getMaxPriceField().clear();
        setRoleFilter(null);
        setMinPriceFilter(null);
        setMaxPriceFilter(null);
        setOffset(0);
        getPlayerContainer().getChildren().clear();
        loadNextPage();
    }
}
