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
        Label placeholder = new Label("Sin configuración cargada");
        placeholder.setStyle("-fx-text-fill: #AAA; -fx-font-size: 14px;");

        getChildren().add(placeholder);
    }

    public MemPanel(Config config) {
        this();
        setConfig(config);
    }

    public void setConfig(Config config) {
        this.config = config;
        getChildren().clear();

        if (config == null) {
            showPlaceholder("Sin configuración cargada");
            return;
        }

        ReplacementType algor = config.getReplacementType();
        int frames = config.getTotalFrames();

        if (frames <= 0) {
            showPlaceholder("Config inválida (totalFrames = " + frames + ")");
            return;
        }

        if (visualizer == null) {
            visualizer = new MemoryVisualizer(algor, frames);
        } else {
            visualizer.initialize(algor, frames);
        }

        drawMemory();  // solo dibuja si hay frames válidos
    }

    private void showPlaceholder(String msg) {
        Label placeholder = new Label(msg);
        placeholder.setStyle("-fx-text-fill: #AAA; -fx-font-size: 14px;");
        getChildren().add(placeholder);
    }

    private void drawMemory() {
        getChildren().clear();

        if (visualizer != null) {
            getChildren().add(visualizer);
        }
    }

    public MemoryVisualizer getVisualizer() {
        return visualizer;
    }
    // Si quieres, un helper para bindear mm directamente:
    public void bindMemoryManager(modules.memory.MemoryManager mm) {
        if (mm != null && visualizer != null) mm.addListener(visualizer);
    }
}