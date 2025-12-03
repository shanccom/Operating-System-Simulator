package modules.gui.dashboard;

import java.util.List;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;
import modules.gui.components.GanttChart;
import modules.gui.components.GanttChartSimple;
import utils.Logger;

public class ExePanel extends VBox implements Logger.PanelHighlightListener {

    private GanttChart ganttChart;
    private GanttChartSimple ganttChartSimple;
    private ScrollPane scrollPane;
    private VBox ganttContainer;
    private boolean useSimpleVersion = false; // false = version original

    
    private VBox contextSwitchLabel;
    
    private PauseTransition exeTimer = new PauseTransition(Duration.millis(300)); //Tiempo de iluminacion

    // Metricas
    private int contextSwitches = 0;
    private double avgWaitTime = 0.0;

    private Button expandButton; // boton para expandir y reducir panel
    private Button switchViewButton; // Boton para cambiar de vista
    private boolean isExpanded = false;
    private Runnable onExpandCallback;
    private Runnable onCollapseCallback;

    public ExePanel() {
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("card-exe");

        // header con titulo y botones de expandir y cambiar
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Diagrama de Gantt - Ejecución");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Boton para cambiar de vista
        switchViewButton = new Button("Vista 2 Líneas");
        switchViewButton.getStyleClass().add("ghost-button");
        switchViewButton.setStyle("-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-color: rgba(33, 150, 243, 0.2); -fx-text-fill: white; -fx-cursor: hand;");
        switchViewButton.setOnAction(e -> switchView());

        expandButton = new Button("+");
        expandButton.getStyleClass().add("ghost-button");
        expandButton.setStyle("-fx-font-size: 16px; -fx-padding: 5 10; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-cursor: hand;");
        expandButton.setOnAction(e -> toggleExpand());

        header.getChildren().addAll(title, spacer, switchViewButton, expandButton);

        // Contenedor con scroll para el Gantt
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Crear ambas versiones del Gantt
        ganttChart = new GanttChart();
        ganttChartSimple = new GanttChartSimple();

        // Envolver el GanttChart en un VBox
        ganttContainer = new VBox();
        ganttContainer.getChildren().add(ganttChart); // Empezar con version original
        ganttContainer.setFillWidth(false);
        ganttContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        ganttContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);

        VBox.setVgrow(ganttContainer, Priority.NEVER);

        // Asigna el contenedor al scrollpane 
        scrollPane.setContent(ganttContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Panel de metricas
        HBox metricsPanel = createMetricsPanel();

        getChildren().addAll(header, scrollPane, metricsPanel);

        // Registrar como listener para iluminarse en logs EXE
        Logger.addPanelListener(this);
    }

    private void switchView() {
        useSimpleVersion = !useSimpleVersion;
        
        // Cambiar el contenido del container
        ganttContainer.getChildren().clear();
        if (useSimpleVersion) {
            ganttContainer.getChildren().add(ganttChartSimple);
            switchViewButton.setText("Vista Multi-Línea");
        } else {
            ganttContainer.getChildren().add(ganttChart);
            switchViewButton.setText("Vista 2 Líneas");
        }
    }

    private void toggleExpand() {
        isExpanded = !isExpanded;
        
        if (isExpanded) {
            expandButton.setText("-"); // Cambiar a icono de reducir
            expandButton.setStyle("-fx-font-size: 16px; -fx-padding: 5 10; -fx-background-color: rgba(76, 175, 80, 0.3); -fx-text-fill: white; -fx-cursor: hand;");
            if (onExpandCallback != null) {
                onExpandCallback.run();
            }
        } else {
            expandButton.setText("+");
            expandButton.setStyle("-fx-font-size: 16px; -fx-padding: 5 10; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-cursor: hand;");
            if (onCollapseCallback != null) {
                onCollapseCallback.run();
            }
        }
    }

    // metodos para configurar callbacks desde dashboard page
    public void setOnExpand(Runnable callback) {
        this.onExpandCallback = callback;
    }

    public void setOnCollapse(Runnable callback) {
        this.onCollapseCallback = callback;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    private HBox createMetricsPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10, 0, 0, 0));

       
        contextSwitchLabel = createMetricBox("Context Switches", "0", "#2196F3");

        panel.getChildren().addAll(contextSwitchLabel);
        return panel;
    }

    private VBox createMetricBox(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle(String.format(
                "-fx-background-color: rgba(255,255,255,0.05); " +
                        "-fx-background-radius: 6px; " +
                        "-fx-min-width: 150px;"));

        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 20px; -fx-font-weight: bold;",
                color));

        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }

    // metodos publico para actualizar gantt y metricas

    //desde que empieza la ejecucion de un proceso

    public void addExecutionStart(String pid, int startTime) {
        ganttChart.addExecutionStart(pid, startTime);
        ganttChartSimple.addExecutionStart(pid, startTime);
        //Auto-scroll al proceso que inicia
        autoScrollToActiveProcess();
    }

    public void addExecutionEnd(String pid, int endTime) {
        ganttChart.addExecutionEnd(pid, endTime);
        ganttChartSimple.addExecutionEnd(pid, endTime);
        updateMetrics();
    }

    public void addIOStart(String pid, int startTime) {
        //System.out.println("[ExePanel] addIOStart: " + pid + " en t=" + startTime);
        ganttChart.addIOStart(pid, startTime);
        ganttChartSimple.addIOStart(pid, startTime);
    }

    public void addIOEnd(String pid, int endTime) {
        //System.out.println("[ExePanel] addIOEnd: " + pid + " en t=" + endTime);
        ganttChart.addIOEnd(pid, endTime);
        ganttChartSimple.addIOEnd(pid, endTime);
    }

    public void addContextSwitchBlock(String pid, int startTime, int duration) {
        ganttChart.addContextSwitchBlock(pid, startTime, duration);
        ganttChartSimple.addContextSwitchBlock(pid, startTime, duration);
    }

    public void setCurrentTime(int time) {
        // System.out.println("[ExePanel] setCurrentTime llamado: " + time);
        ganttChart.setCurrentTime(time);
        ganttChartSimple.setCurrentTime(time);
        //Auto-scroll para seguir el cursor
        autoScrollToCurrentTime();
    }

    // metodo para hacer auto-scroll siguiendo el cursor
    private void autoScrollToCurrentTime() {
        Platform.runLater(() -> {
            double cursorX = useSimpleVersion ? ganttChartSimple.getCurrentCursorX() : ganttChart.getCurrentCursorX();
            double totalWidth = useSimpleVersion ? ganttChartSimple.getTotalWidth() : ganttChart.getTotalWidth();
            double viewportWidth = scrollPane.getViewportBounds().getWidth();
            
            double targetHScrollPosition = (cursorX - (viewportWidth * 0.7)) / (totalWidth - viewportWidth);
            targetHScrollPosition = Math.max(0, Math.min(1, targetHScrollPosition));
            
            double currentHScrollPosition = scrollPane.getHvalue();
            double cursorRelativePositionH = (cursorX - (currentHScrollPosition * (totalWidth - viewportWidth))) / viewportWidth;
            
            if (cursorRelativePositionH > 0.8 || cursorRelativePositionH < 0.1) {
                scrollPane.setHvalue(targetHScrollPosition);
            }
            
            // scroll vertical solo para version multi-linea
            if (!useSimpleVersion) {
                int activeRow = ganttChart.getActiveProcessRow();
                
                if (activeRow >= 0) {
                    double processY = ganttChart.getActiveProcessY();
                    double totalHeight = ganttChart.getTotalHeight();
                    double viewportHeight = scrollPane.getViewportBounds().getHeight();
                    
                // Calcular la posición vertical del scroll para centrar el proceso
                    double targetVScrollPosition = (processY - (viewportHeight * 0.5)) / (totalHeight - viewportHeight);
                    targetVScrollPosition = Math.max(0, Math.min(1, targetVScrollPosition));
                    
                // Verificar si el proceso está fuera de vista
                    double currentVScrollPosition = scrollPane.getVvalue();
                    double processRelativePositionV = (processY - (currentVScrollPosition * (totalHeight - viewportHeight))) / viewportHeight;
                    
                // Si el proceso está fuera del 20%-80% del viewport, hacer scroll
                    if (processRelativePositionV < 0.2 || processRelativePositionV > 0.8) {
                        scrollPane.setVvalue(targetVScrollPosition);
                    }
                }
            }
        });
    }
    // metodo para hacer scroll inmediato al proceso activo
    private void autoScrollToActiveProcess() {
        if (useSimpleVersion) return;
        
        Platform.runLater(() -> {
            int activeRow = ganttChart.getActiveProcessRow();
            
            if (activeRow >= 0) {
                double processY = ganttChart.getActiveProcessY();
                double totalHeight = ganttChart.getTotalHeight();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                
                // Centrar el proceso en el viewport
                double targetVScrollPosition = (processY - (viewportHeight * 0.5)) / (totalHeight - viewportHeight);
                targetVScrollPosition = Math.max(0, Math.min(1, targetVScrollPosition));
                
                scrollPane.setVvalue(targetVScrollPosition);
                
                //System.out.println("[ExePanel]Auto-scroll a proceso activo: fila " + activeRow);
            }
        });
    }


    public void incrementContextSwitch() {
        // System.out.println("[ExePanel] incrementContextSwitch llamado");
        contextSwitches++;
        updateMetrics();
    }


    public void setAvgWaitTime(double waitTime) {
        this.avgWaitTime = waitTime;
        updateMetrics();
    }

    public void clearGantt() {
        ganttChart.clear();
        ganttChartSimple.clear();
        contextSwitches = 0;
        avgWaitTime = 0.0;
        updateMetrics();
        
        // resetear scroll al inicio
        Platform.runLater(() -> {
            scrollPane.setHvalue(0);
            scrollPane.setVvalue(0);
        });
    }

    public void initializeProcesses(List<String> processIds) {
        ganttChart.initializeProcesses(processIds);
        ganttChartSimple.initializeProcesses(processIds);
    }

    private void updateMetrics() {
        Platform.runLater(() -> {
            

            Label csValue = (Label) ((VBox) contextSwitchLabel).getChildren().get(1);
            csValue.setText(String.valueOf(contextSwitches));

        });
    }

    // Implementación de PanelHighlightListener
    @Override
    public void onLogEmitted(Logger.LogLevel level) {
        if (level == Logger.LogLevel.EXE) {
            highlight();
        }
    }

    public void highlight() {
        Platform.runLater(() -> {
            if (!getStyleClass().contains("card-exe-active")) {
                getStyleClass().add("card-exe-active");
            }
        });
    }

    public void clearHighlight() {
        Platform.runLater(() -> {
            getStyleClass().remove("card-exe-active");
        });
    }


}