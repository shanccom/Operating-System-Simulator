package modules.gui.dashboard;

import java.util.List;

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

    // Metricas
    private int totalCPUTime = 0;
    private int totalIdleTime = 0;
    private int contextSwitches = 0;
    private double avgWaitTime = 0.0;

    public ExePanel() {
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("card-exe");

        Label title = new Label("Diagrama de Gantt - Ejecucion");
        title.getStyleClass().add("card-title");

        // Contenedor con scroll para el Gantt
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        ganttChart = new GanttChart();
        scrollPane.setContent(ganttChart);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Panel de metricas
        HBox metricsPanel = createMetricsPanel();

        getChildren().addAll(title, scrollPane, metricsPanel);
    }

    private HBox createMetricsPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10, 0, 0, 0));

        cpuUtilLabel = createMetricBox("CPU Utilization", "0%", "#4CAF50");
        contextSwitchLabel = createMetricBox("Context Switches", "0", "#2196F3");
        avgWaitLabel = createMetricBox("Avg Wait Time", "0u", "#FF9800");

        panel.getChildren().addAll(cpuUtilLabel, contextSwitchLabel, avgWaitLabel);
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

    // metodos publico para actualizar gantt y metricas
    
    /*public void addExecution(String pid, int startTime, int endTime) {
        //System.out.println("[ExePanel] addExecution llamado: " + pid + " [" + startTime + "-" + endTime + "]");
        ganttChart.addExecution(pid, startTime, endTime);
        totalCPUTime += (endTime - startTime);
        updateMetrics();
    }*/
    //desde que empieza la ejecucion de un proceso
    public void addExecutionStart(String pid, int startTime) {
        ganttChart.addExecutionStart(pid, startTime);
    }
    public void addExecutionEnd(String pid, int endTime) {
        ganttChart.addExecutionEnd(pid, endTime);
        totalCPUTime += 1; // Ajustar según la duración real
        updateMetrics();
    }

    public void setCurrentTime(int time) {
        //System.out.println("[ExePanel] setCurrentTime llamado: " + time);
        ganttChart.setCurrentTime(time);
    }

    public void incrementContextSwitch() {
        //System.out.println("[ExePanel] incrementContextSwitch llamado");
        contextSwitches++;
        updateMetrics();
    }

    public void setIdleTime(int idleTime) {
        this.totalIdleTime = idleTime;
        updateMetrics();
    }

    public void setAvgWaitTime(double waitTime) {
        this.avgWaitTime = waitTime;
        updateMetrics();
    }

    public void clearGantt() {
        ganttChart.clear();
        totalCPUTime = 0;
        totalIdleTime = 0;
        contextSwitches = 0;
        avgWaitTime = 0.0;
        updateMetrics();
    }

    public void initializeProcesses(List<String> processIds) {
        ganttChart.initializeProcesses(processIds);
    }


    private void updateMetrics() {
        Platform.runLater(() -> {
            int totalTime = totalCPUTime + totalIdleTime;
            double cpuUtil = totalTime > 0 ? (totalCPUTime * 100.0 / totalTime) : 0;
            
            Label cpuValue = (Label) ((VBox) cpuUtilLabel).getChildren().get(1);
            cpuValue.setText(String.format("%.1f%%", cpuUtil));
            
            Label csValue = (Label) ((VBox) contextSwitchLabel).getChildren().get(1);
            csValue.setText(String.valueOf(contextSwitches));
            
            Label waitValue = (Label) ((VBox) avgWaitLabel).getChildren().get(1);
            waitValue.setText(String.format("%.1fu", avgWaitTime));
        });
    }
}