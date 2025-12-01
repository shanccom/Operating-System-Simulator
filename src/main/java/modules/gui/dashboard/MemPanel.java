package modules.gui.dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import modules.gui.components.MemoryVisualizer;
import model.Config;
import model.Config.ReplacementType;

public class MemPanel extends VBox {

    private Config config;
    private MemoryVisualizer visualizer;

    public MemPanel() {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);
        
        // Crear el visualizer vacío desde el inicio
        visualizer = new MemoryVisualizer();
        getChildren().add(visualizer);
    }

    public MemPanel(Config config) {
        this();
        setConfig(config);
    }

    public void setConfig(Config config) {
        this.config = config;

        if (config == null) {
            // Reiniciar el visualizer a estado vacío
            if (visualizer != null) {
                getChildren().clear();
                visualizer = new MemoryVisualizer();
                getChildren().add(visualizer);
            }
            return;
        }

        ReplacementType algor = config.getReplacementType();
        int frames = config.getTotalFrames();

        if (frames <= 0) {
            // Mostrar error en el visualizer
            showPlaceholder("Config inválida (totalFrames = " + frames + ")");
            return;
        }

        // Inicializar el visualizer con la configuración válida
        if (visualizer != null) {
            visualizer.initialize(algor, frames);
        }
    }

    private void showPlaceholder(String msg) {
        getChildren().clear();
        Label placeholder = new Label(msg);
        placeholder.setStyle("-fx-text-fill: #AAA; -fx-font-size: 14px;");
        getChildren().add(placeholder);
    }

    public MemoryVisualizer getVisualizer() {
        return visualizer;
    }

    // Helper para bindear el MemoryManager directamente
    public void bindMemoryManager(modules.memory.MemoryManager mm) {
        if (mm != null && visualizer != null) {
            mm.addListener(visualizer);
        }
    }
}