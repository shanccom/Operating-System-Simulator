package modules.gui.dashboard;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Process;
import utils.Logger;

import java.util.List;

public class ProPanel extends VBox implements Logger.PanelHighlightListener {

    private VBox readyContainer;
    private VBox blockedIOContainer;
    private VBox blockedMemoryContainer;

    public ProPanel() {
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("card-proc");

        Label title = new Label("Colas de Procesos");
        title.getStyleClass().add("card-title");

        // Contenedor con scroll
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Contenedor principal en HORIZONTAL (COLUMNAS)
        HBox content = new HBox(24);
        content.setPadding(new Insets(8));
        content.setAlignment(Pos.TOP_LEFT);

        // COLUMNA READY
        Label readyTitle = new Label("Ready");
        readyTitle.getStyleClass().add("section-title");
        readyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        readyContainer = new VBox(6);
        readyContainer.setAlignment(Pos.TOP_LEFT);

        VBox colReady = new VBox(10, readyTitle, readyContainer);
        colReady.setPrefWidth(180);

        // COLUMNA BLOCKED I/O
        Label blockedIOTitle = new Label("Blocked (I/O)");
        blockedIOTitle.getStyleClass().add("section-title");
        blockedIOTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FF9800;");

        blockedIOContainer = new VBox(6);
        blockedIOContainer.setAlignment(Pos.TOP_LEFT);

        VBox colIO = new VBox(10, blockedIOTitle, blockedIOContainer);
        colIO.setPrefWidth(180);

        // COLUMNA BLOCKED MEMORY
        Label blockedMemTitle = new Label("Blocked (Memory)");
        blockedMemTitle.getStyleClass().add("section-title");
        blockedMemTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F44336;");

        blockedMemoryContainer = new VBox(6);
        blockedMemoryContainer.setAlignment(Pos.TOP_LEFT);

        VBox colMem = new VBox(10, blockedMemTitle, blockedMemoryContainer);
        colMem.setPrefWidth(180);

        // Agregar columnas al HBox
        content.getChildren().addAll(colReady, colIO, colMem);

        scrollPane.setContent(content);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(title, scrollPane);

        // Mostrar valores iniciales vacíos
        updateReadyQueue(List.of());
        updateBlockedIO(List.of());
        updateBlockedMemory(List.of());

        // Registrar como listener para iluminarse en logs PROC
        Logger.addPanelListener(this);
    }

    // MÉTODOS PARA ACTUALIZAR COLAS
    public void updateReadyQueue(List<Process> processes) {

        // System.out.println("[UI]
        // ------------------------------------------------***************************READY
        // recibido: " + processes.size());
        for (Process p : processes) {
            // System.out.println(" READY -> " + p.getPid() + " | state=" + p.getState());
        }

        Platform.runLater(() -> {
            readyContainer.getChildren().clear();

            if (processes.isEmpty()) {
                readyContainer.getChildren().add(emptyLabel());
            } else {
                for (Process p : processes)
                    readyContainer.getChildren().add(createProcessBadge(p, "#4CAF50"));
            }
        });
    }

    public void updateBlockedIO(List<Process> processes) {
        Platform.runLater(() -> {
            blockedIOContainer.getChildren().clear();

            if (processes.isEmpty()) {
                blockedIOContainer.getChildren().add(emptyLabel());
            } else {
                for (Process p : processes)
                    blockedIOContainer.getChildren().add(createProcessBadge(p, "#FF9800"));
            }
        });
    }

    public void updateBlockedMemory(List<Process> processes) {
        Platform.runLater(() -> {
            blockedMemoryContainer.getChildren().clear();

            if (processes.isEmpty()) {
                blockedMemoryContainer.getChildren().add(emptyLabel());
            } else {
                for (Process p : processes)
                    blockedMemoryContainer.getChildren().add(createProcessBadge(p, "#F44336"));
            }
        });
    }

    // ELEMENTOS VISUALES
    private Label emptyLabel() {
        Label empty = new Label("— Cola vacía —");
        empty.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.3); -fx-font-size: 11px;");
        return empty;
    }

    private HBox createProcessBadge(Process process, String color) {
        HBox badge = new HBox(8);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(6, 10, 6, 10));
        badge.setStyle(String.format(
                "-fx-background-color: %s; " +
                        "-fx-background-radius: 6px; -fx-border-radius: 6px;",
                color));

        // Icono
        Region icon = new Region();
        icon.setPrefSize(8, 8);
        icon.setStyle("-fx-background-color: white; -fx-background-radius: 50%;");

        // Información
        VBox info = new VBox(2);

        Label pidLabel = new Label(process.getPid());
        pidLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label detailLabel = new Label(
                "Prioridad: " + process.getPriority() +
                        " | Espera: " + process.getWaitingTime());
        detailLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 10px;");

        info.getChildren().addAll(pidLabel, detailLabel);

        badge.getChildren().addAll(icon, info);
        return badge;
    }

    // Implementación de PanelHighlightListener
    @Override
    public void onLogEmitted(Logger.LogLevel level) {
        if (level == Logger.LogLevel.PROC) {
            highlight();
        }
    }

    private void highlight() {
        Platform.runLater(() -> {
            getStyleClass().add("card-proc-active");

            PauseTransition pause = new PauseTransition(Duration.millis(800));
            pause.setOnFinished(e -> getStyleClass().remove("card-proc-active"));
            pause.play();
        });
    }
}
