package modules.gui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class SimulationControls extends HBox {
    
    private final Button startButton;
    private final Button pauseButton;
    private final Button resumeButton;
    private final Button stepButton;
    private final Label statusLabel;
    
    private SimulationControlListener listener;
    
    public interface SimulationControlListener {
        void onStart();
        void onPause();
        void onResume();
        void onStep();
    }
    
    public SimulationControls() {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("simulation-controls");
        
        // Botón Iniciar
        startButton = new Button("Iniciar Simulación");
        startButton.getStyleClass().add("primary-button");
        startButton.setOnAction(e -> handleStart());
        
        // Botón Pausar
        pauseButton = new Button("Pausar");
        pauseButton.getStyleClass().add("secondary-button");
        pauseButton.setOnAction(e -> handlePause());
        pauseButton.setDisable(true);
        
        // Botón Reanudar
        resumeButton = new Button("Reanudar");
        resumeButton.getStyleClass().add("primary-button");
        resumeButton.setOnAction(e -> handleResume());
        resumeButton.setDisable(true);
        resumeButton.setVisible(false);
        
        // Botón Paso a Paso
        stepButton = new Button("Siguiente Paso");
        stepButton.getStyleClass().add("secondary-button");
        stepButton.setOnAction(e -> handleStep());
        stepButton.setDisable(true);
        
        // Etiqueta de estado
        statusLabel = new Label("Listo para iniciar");
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        getChildren().addAll(
            startButton,
            pauseButton,
            resumeButton,
            stepButton,
            spacer,
            statusLabel
        );
    }
    
    public void setListener(SimulationControlListener listener) {
        this.listener = listener;
    }
    
    private void handleStart() {
        if (listener != null) {
            listener.onStart();
        }
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        stepButton.setDisable(false);
        statusLabel.setText("Simulación iniciada (en pausa)");
        statusLabel.setStyle("-fx-text-fill: #FFA500; -fx-font-size: 12px;");
    }
    
    private void handlePause() {
        if (listener != null) {
            listener.onPause();
        }
        pauseButton.setVisible(false);
        pauseButton.setDisable(true);
        resumeButton.setVisible(true);
        resumeButton.setDisable(false);
        stepButton.setDisable(false);
        statusLabel.setText("Simulación pausada");
        statusLabel.setStyle("-fx-text-fill: #FFA500; -fx-font-size: 12px;");
    }
    
    private void handleResume() { //Boton continuar todo rapido
        if (listener != null) {
            listener.onResume();
        }
        resumeButton.setVisible(false);
        resumeButton.setDisable(true);
        pauseButton.setVisible(true);
        pauseButton.setDisable(false);
        stepButton.setDisable(true);
        statusLabel.setText("Simulación en curso...");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
    }
    
    private void handleStep() {
        if (listener != null) {
            listener.onStep();
        }
        statusLabel.setText("Ejecutando paso...");
    }
    
    public void setStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
    }
    
    public void simulationCompleted() {
        Platform.runLater(() -> {
            startButton.setDisable(false);
            pauseButton.setDisable(true);
            pauseButton.setVisible(true);
            resumeButton.setDisable(true);
            resumeButton.setVisible(false);
            stepButton.setDisable(true);
            statusLabel.setText("Simulación completada");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
        });
    }
}