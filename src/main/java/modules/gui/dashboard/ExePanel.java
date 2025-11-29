package modules.gui.dashboard;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import modules.gui.components.GanttChart;


public class ExePanel extends VBox {

    private GanttChart ganttChart;

    private VBox cpuUtilLabel;
    private VBox contextSwitchLabel;
    private VBox avgWaitLabel;

    public ExePanel() {
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("card");

        Label title = new Label("Diagrama de Gantt - Ejecución");
        title.getStyleClass().add("card-title");

        // Contenedor con scroll para el Gantt
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        ganttChart = new GanttChart();
        scrollPane.setContent(ganttChart);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Panel de métricas
        HBox metricsPanel = createMetricsPanel();

        getChildren().addAll(title, scrollPane, metricsPanel);
    }

    private HBox createMetricsPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10, 0, 0, 0));

        //cpuUtilLabel = createMetricBox("CPU Utilization", "0%", "#4CAF50");
        //contextSwitchLabel = createMetricBox("Context Switches", "0", "#2196F3");
        //avgWaitLabel = createMetricBox("Avg Wait Time", "0u", "#FF9800");

        //panel.getChildren().addAll(cpuUtilLabel, contextSwitchLabel, avgWaitLabel);
        return panel;
    }

    private VBox createMetricBox(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle(String.format(
            "-fx-background-color: rgba(255,255,255,0.05); " +
            "-fx-background-radius: 6px; " +
            "-fx-min-width: 150px;"
        ));

        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(String.format(
            "-fx-text-fill: %s; -fx-font-size: 20px; -fx-font-weight: bold;",
            color
        ));

        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }

    // MÉTODOS PÚBLICOS PARA ACTUALIZAR EL GANTT
    
    public void addExecution(String pid, int startTime, int endTime) {
        System.out.println("[ExePanel] ✅ addExecution llamado: " + pid + " [" + startTime + "-" + endTime + "]");
        ganttChart.addExecution(pid, startTime, endTime);
    }

    public void setCurrentTime(int time) {
        System.out.println("[ExePanel] ✅ setCurrentTime llamado: " + time);
        ganttChart.setCurrentTime(time);
    }

}