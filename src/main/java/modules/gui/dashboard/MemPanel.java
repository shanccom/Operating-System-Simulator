package modules.gui.dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    }

    public MemPanel(Config config) {
        this();
        setConfig(config);
    }

    public void setConfig(Config config) {
        this.config = config;

        int frames = config.getTotalFrames();
        ReplacementType algor = config.getReplacementType();

        if (visualizer == null) {
            visualizer = new MemoryVisualizer(frames, algor);
        } else {
            visualizer.initialize(frames, algor);
        }

        drawMemory();
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