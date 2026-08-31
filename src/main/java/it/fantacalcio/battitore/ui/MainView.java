package it.fantacalcio.battitore.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import it.fantacalcio.battitore.model.Player;
import it.fantacalcio.battitore.model.PlayerStatus;
import it.fantacalcio.battitore.service.AuctionService;
import it.fantacalcio.battitore.service.AuctionStateService;
import it.fantacalcio.battitore.service.FantacalcioExcelReader;
import it.fantacalcio.battitore.service.SpeechService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView extends BorderPane {

    private final Stage stage;
    private final FantacalcioExcelReader reader = new FantacalcioExcelReader();
    private final AuctionService auction = new AuctionService();
    private final SpeechService speech = new SpeechService();
    private final AuctionStateService stateService = new AuctionStateService();

    private final ComboBox<String> roleCombo = new ComboBox<>();

    private final Label fileLabel = new Label("Nessun listone caricato");
    private final Label playerName = new Label("CARICA IL LISTONE");
    private final Label playerRole = new Label("—");
    private final Label playerTeam = new Label("—");

    private final Label availableStatsLabel = new Label("Disponibili: 0");
    private final Label soldStatsLabel = new Label("Aggiudicati: 0");
    private final Label unsoldStatsLabel = new Label("Invenduti: 0");

    private final Label statusLabel = new Label("Carica il file Excel di Fantacalcio.it per iniziare.");

    private final CheckBox readRole = new CheckBox("Leggi ruolo");
    private final CheckBox readTeam = new CheckBox("Leggi squadra");

    private final Button repeatButton = new Button("🔊  RIPETI");
    private final Button soldButton = new Button("✓  AGGIUDICATO");
    private final Button unsoldButton = new Button("↩  INVENDUTO");
    private final Button nextButton = new Button("PROSSIMO  →");
    private final Button replayUnsoldButton = new Button("Ripassa invenduti");
    private final Button saveStateButton = new Button("Salva stato");
    private final Button loadStateButton = new Button("Carica stato");
    private final Button viewAllPlayersButton = new Button("Visualizza Listone Caricato");

    private File loadedExcelFile;

    public MainView(Stage stage) {
        this.stage = stage;
        buildUi();
        wireActions();
        updateControls();

        Platform.runLater(() -> stage.setOnCloseRequest(event -> speech.close()));
    }

    private void buildUi() {
        setPadding(new Insets(22));
        setTop(buildTop());
        setCenter(buildCenter());
        setBottom(buildBottom());
    }

    private Pane buildTop() {
        Button loadButton = new Button("Apri listone Excel");
        loadButton.setId("load-button");
        loadButton.setOnAction(event -> chooseExcelFile());

        roleCombo.getItems().addAll(AuctionService.ALL_ROLES, "P", "D", "C", "A");
        roleCombo.setValue(AuctionService.ALL_ROLES);
        roleCombo.setPrefWidth(135);

        HBox controls = new HBox(12,
                loadButton,
                new Separator(),
                new Label("Ruolo:"),
                roleCombo,
                readRole,
                readTeam
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        readRole.setSelected(true);
        readTeam.setSelected(true);

        VBox top = new VBox(10, controls, fileLabel);
        top.setPadding(new Insets(0, 0, 18, 0));
        return top;
    }

    private Pane buildCenter() {
        playerName.setId("player-name");
        playerRole.setId("player-role");
        playerTeam.setId("player-team");

        configureClickableStatsLabel(availableStatsLabel);
        configureClickableStatsLabel(soldStatsLabel);
        configureClickableStatsLabel(unsoldStatsLabel);

        HBox statsBox = new HBox(
                18,
                availableStatsLabel,
                soldStatsLabel,
                unsoldStatsLabel
        );
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setId("stats-label");

        VBox card = new VBox(10, playerName, playerRole, playerTeam, statsBox);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setId("player-card");
        VBox.setVgrow(card, Priority.ALWAYS);

        return card;
    }

    private void configureClickableStatsLabel(Label label) {
        label.setCursor(Cursor.HAND);
        label.setUnderline(false);

        label.setOnMouseEntered(event -> label.setUnderline(true));
        label.setOnMouseExited(event -> label.setUnderline(false));
    }

    private Pane buildBottom() {
        repeatButton.getStyleClass().add("secondary-button");
        soldButton.getStyleClass().add("success-button");
        unsoldButton.getStyleClass().add("warning-button");
        nextButton.getStyleClass().add("primary-button");

        HBox auctionButtons = new HBox(14, repeatButton, unsoldButton, soldButton, nextButton);
        auctionButtons.setAlignment(Pos.CENTER);

        HBox utilityButtons = new HBox(10, viewAllPlayersButton, replayUnsoldButton, saveStateButton, loadStateButton);
        utilityButtons.setAlignment(Pos.CENTER_LEFT);

        statusLabel.setId("status-label");
        statusLabel.setWrapText(true);

        VBox bottom = new VBox(14, auctionButtons, new Separator(), utilityButtons, statusLabel);
        bottom.setPadding(new Insets(18, 0, 0, 0));
        return bottom;
    }

    private void wireActions() {
        roleCombo.setOnAction(event -> {
            showNoCurrentPlayer();
            updateStats();
            updateControls();
        });

        availableStatsLabel.setOnMouseClicked(event ->
                showPlayersByStatus("Giocatori disponibili", PlayerStatus.AVAILABLE));

        soldStatsLabel.setOnMouseClicked(event ->
                showPlayersByStatus("Giocatori aggiudicati", PlayerStatus.SOLD));

        unsoldStatsLabel.setOnMouseClicked(event ->
                showPlayersByStatus("Giocatori invenduti", PlayerStatus.UNSOLD));

        repeatButton.setOnAction(event -> speakCurrent());

        nextButton.setOnAction(event -> {
            // Se si passa oltre senza assegnare il giocatore corrente,
            // lo consideriamo invenduto per evitare che venga richiamato subito.
            if (auction.getCurrentPlayer() != null && auction.getCurrentPlayer().isAvailable()) {
                auction.markCurrentUnsold();
            }
            drawNext();
        });

        soldButton.setOnAction(event -> {
            auction.markCurrentSold();
            statusLabel.setText("Giocatore segnato come aggiudicato.");
            drawNext();
        });

        unsoldButton.setOnAction(event -> {
            auction.markCurrentUnsold();
            statusLabel.setText("Giocatore messo tra gli invenduti.");
            drawNext();
        });

        replayUnsoldButton.setOnAction(event -> {
            int reactivated = auction.reactivateUnsold(selectedRole());
            if (reactivated == 0) {
                statusLabel.setText("Non ci sono invenduti da rimettere in gioco per questo ruolo.");
            } else {
                statusLabel.setText("Rimessi in gioco " + reactivated + " giocatori invenduti.");
            }
            showNoCurrentPlayer();
            updateStats();
            updateControls();
        });

        saveStateButton.setOnAction(event -> saveState());
        loadStateButton.setOnAction(event -> loadState());
        viewAllPlayersButton.setOnAction(event -> showAllPlayersWithStatus("Listone completo"));
    }

    private void chooseExcelFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona il listone Fantacalcio");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("File Excel", "*.xlsx", "*.xls")
                //,new FileChooser.ExtensionFilter("Tutti i file", "*.*")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            List<Player> players = reader.read(file);
            auction.loadPlayers(players);
            loadedExcelFile = file;
            fileLabel.setText(file.getName() + "  •  " + players.size() + " giocatori caricati");
            statusLabel.setText("Listone caricato. Premi PROSSIMO per chiamare il primo giocatore.");
            showNoCurrentPlayer();
            updateStats();
            updateControls();
            roleCombo.setValue("P");
        } catch (Exception ex) {
            showError("Errore lettura listone", ex.getMessage());
        }
    }

    private void drawNext() {
        Player player = auction.drawNext(selectedRole());
        if (player == null) {
            showNoCurrentPlayer();
            int unsold = auction.countUnsold(selectedRole());
            statusLabel.setText(unsold > 0
                    ? "Nessun altro giocatore disponibile. Ci sono " + unsold + " invenduti: usa 'Ripassa invenduti'."
                    : "Nessun altro giocatore disponibile per il ruolo selezionato.");
        } else {
            showPlayer(player);
            statusLabel.setText("Giocatore chiamato. AGGIUDICATO lo rimuove; PROSSIMO lo considera invenduto e passa oltre.");
            speakCurrent();
        }
        updateStats();
        updateControls();
    }

    private void showPlayer(Player player) {
        playerName.setText(player.getName().toUpperCase());
        playerRole.setText(roleLongName(player.getRole()));
        playerTeam.setText(player.getTeam().toUpperCase());
    }

    private void showNoCurrentPlayer() {
        playerName.setText(auction.getPlayers().isEmpty() ? "CARICA IL LISTONE" : "PRONTO");
        playerRole.setText("—");
        playerTeam.setText("—");
    }

    private void speakCurrent() {
        speech.speak(auction.getCurrentPlayer(), readRole.isSelected(), readTeam.isSelected());
    }

    private void updateStats() {
        String role = selectedRole();

        availableStatsLabel.setText("Disponibili: " + countPlayersByStatus(PlayerStatus.AVAILABLE, role));
        soldStatsLabel.setText("Aggiudicati: " + countPlayersByStatus(PlayerStatus.SOLD, role));
        unsoldStatsLabel.setText("Invenduti: " + countPlayersByStatus(PlayerStatus.UNSOLD, role));
    }

    private int countPlayersByStatus(PlayerStatus status, String role) {
        return (int) auction.getPlayers().stream()
                .filter(player -> player.getStatus() == status)
                .filter(player -> matchesRole(player, role))
                .count();
    }

    private void showPlayersByStatus(String title, PlayerStatus status) {
        String role = selectedRole();

        if(role.equalsIgnoreCase(AuctionService.ALL_ROLES)){
            return;
        }

        List<Player> players = auction.getAllByStatusAndRole(status, role);

        if(players == null || players.isEmpty()) {
            return;
        }
        
        players.sort(Comparator.comparing(Player::getFvm).reversed());

        Set<Player> selectedPlayers = new HashSet<>();

        ListView<Player> listView = new ListView<>();
        listView.getItems().setAll(players);
        listView.setPrefWidth(520);
        listView.setPrefHeight(460);

        listView.setCellFactory(view -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final HBox content = new HBox(10, checkBox, label);
            {
                content.setAlignment(Pos.CENTER_LEFT);

                checkBox.setOnAction(event -> {
                    Player player = getItem();

                    if (player == null) {
                        return;
                    }

                    if (checkBox.isSelected()) {
                        selectedPlayers.add(player);
                    } else {
                        selectedPlayers.remove(player);
                    }
                });
            }

            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);

                if (empty || player == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                label.setText(
                        player.getName()
                        + "   •   " + player.getRole()
                        + "   •   " + player.getTeam()
                        + "   •   " + player.getFvm()
                );

                checkBox.setSelected(selectedPlayers.contains(player));

                setText(null);
                setGraphic(content);
            }
        });

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title + " (" + players.size() + ")" + ( " - Ruolo " + role) + ( " - Stato " + status.name()));
        alert.setContentText(null);
        alert.getDialogPane().setContent(listView);
        alert.setResizable(true);
        alert.showAndWait();
        if(status != PlayerStatus.AVAILABLE){
            if(!selectedPlayers.isEmpty()){
                ListView<Player> listViewSel = new ListView<>();
                listViewSel.getItems().setAll(selectedPlayers);
                listViewSel.setPrefWidth(520);
                listViewSel.setPrefHeight(460);
                Alert alert1 = new Alert(AlertType.CONFIRMATION,"",ButtonType.YES,ButtonType.NO);
                alert1.initOwner(stage);
                alert1.setTitle("Vuoi rimettere in gioco i giocatori selezionati?");
                alert1.setHeaderText(title + " (" + selectedPlayers.size() + ")" + ( " - Ruolo " + role) + ( " - Stato " + status.name()));
                alert1.setContentText(null);
                alert1.getDialogPane().setContent(listViewSel);
                alert1.setResizable(true);
                Optional<ButtonType> result = alert1.showAndWait();
                if(result.isPresent() && result.get() == ButtonType.YES){
                    for(Player player : selectedPlayers){
                        player.setStatus(PlayerStatus.AVAILABLE);
                    }
                    statusLabel.setText("Rimessi in gioco " + selectedPlayers.size() + " giocatori.");
                    updateStats();
                    updateControls();
                }
            }
        }
    }

    private void showAllPlayersWithStatus(String title) {

        List<Player> players = auction.getPlayers().stream().collect(Collectors.toList());

        if(players == null || players.isEmpty()) {
            return;
        }

        players.sort(Comparator.comparing(Player::getRole).thenComparing(Player::getFvm).reversed());
        
        ListView<Player> listView = new ListView<>();
        listView.getItems().setAll(players);
        listView.setPrefWidth(520);
        listView.setPrefHeight(460);

        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);

                if (empty || player == null) {
                    setText(null);
                    return;
                }

                setText(player.getName()
                        + "   •   " + player.getRole()
                        + "   •   " + player.getTeam()
                        + "   •   " + player.getFvm()
                        + "   •   " + player.getStatus().name());
            }
        });

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title + " (" + players.size() + ") " + " - Ruolo " + AuctionService.ALL_ROLES);
        alert.setContentText(null);
        alert.getDialogPane().setContent(listView);
        alert.setResizable(true);
        alert.showAndWait();
    }


    private boolean matchesRole(Player player, String role) {
        return role == null
                || role.isBlank()
                || AuctionService.ALL_ROLES.equalsIgnoreCase(role)
                || player.getRole().equalsIgnoreCase(role);
    }

    private void updateControls() {
        boolean hasPlayers = !auction.getPlayers().isEmpty();
        boolean hasCurrent = auction.getCurrentPlayer() != null;

        nextButton.setDisable(!hasPlayers || auction.countAvailable(selectedRole()) == 0);
        repeatButton.setDisable(!hasCurrent);
        soldButton.setDisable(!hasCurrent);
        unsoldButton.setDisable(!hasCurrent);
        replayUnsoldButton.setDisable(!hasPlayers || auction.countUnsold(selectedRole()) == 0);
        saveStateButton.setDisable(!hasPlayers);
        loadStateButton.setDisable(!hasPlayers);
        viewAllPlayersButton.setDisable(!hasPlayers);
    }

    private void saveState() {
        if (auction.getPlayers().isEmpty()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salva stato asta");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Stato Battitore", "*.fasta")
        );
        chooser.setInitialFileName(defaultStateFileName());

        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        Path path = ensureExtension(file.toPath(), ".fasta");
        try {
            stateService.save(path, auction.getPlayers());
            statusLabel.setText("Stato salvato in " + path.getFileName() + ".");
        } catch (Exception ex) {
            showError("Errore salvataggio", ex.getMessage());
        }
    }

    private void loadState() {
        if (auction.getPlayers().isEmpty()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Carica stato asta");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Stato Battitore", "*.fasta")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            int restored = stateService.load(file.toPath(), auction.getPlayers());
            statusLabel.setText("Stato ripristinato per " + restored + " giocatori.");
            showNoCurrentPlayer();
            updateStats();
            updateControls();
        } catch (Exception ex) {
            showError("Errore caricamento stato", ex.getMessage());
        }
    }

    private String defaultStateFileName() {
        if (loadedExcelFile == null) {
            return "asta.fasta";
        }
        String name = loadedExcelFile.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name + ".fasta";
    }

    private Path ensureExtension(Path path, String extension) {
        String fileName = path.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(extension)) {
            return path.resolveSibling(fileName + extension);
        }
        return path;
    }

    private String selectedRole() {
        String role = roleCombo.getValue();
        return role == null ? AuctionService.ALL_ROLES : role;
    }

    private String roleLongName(String role) {
        return switch (role == null ? "" : role.toUpperCase()) {
            case "P" -> "PORTIERE";
            case "D" -> "DIFENSORE";
            case "C" -> "CENTROCAMPISTA";
            case "A" -> "ATTACCANTE";
            default -> role == null ? "" : role;
        };
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Errore non specificato." : message);
        alert.showAndWait();
    }
}
