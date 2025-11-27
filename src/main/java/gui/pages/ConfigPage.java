package gui.pages;

import gui.SimulationRunner;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ConfigPage extends VBox {

    private File configFile = new File("src/main/resources/data/config.txt");
    private File processFile = new File("src/main/resources/data/procesos.txt");

    private final Label labelConfig = new Label();
    private final Label labelProcess = new Label();
    private final Label labelStatus = new Label();

    public ConfigPage(Stage stage) {

        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("page-container");

        labelConfig.setText("Config: " + configFile.getName());
        labelProcess.setText("Procesos: " + processFile.getName());
        labelStatus.setText("Listo para iniciar");

        VBox sectionFiles = buildFileSection(stage);
        VBox sectionCPU = buildCpuSection();
        VBox sectionMemory = buildMemorySection();
        Button startButton = buildStartButton();

        getChildren().addAll(
                sectionFiles,
                sectionCPU,
                sectionMemory,
                startButton,
                labelStatus
        );
    }

    private VBox buildFileSection(Stage stage) {
        VBox box = new VBox(10);
        return box;
    }

    private VBox buildCpuSection() {
        VBox box = new VBox(10);
        return box;
    }

    private VBox buildMemorySection() {
        VBox box = new VBox(10);
        return box;
    }

    private Button buildStartButton() {
        Button btn = new Button("Start Simulation");
        btn.setOnAction(e -> runSimulation(labelStatus));
        return btn;
    }

    private File openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("TXT files", "*.txt")
        );
        return fc.showOpenDialog(stage);
    }

    private void runSimulation(Label status) {
        try {
            SimulationRunner.runSimulation(
                    configFile.getAbsolutePath(),
                    processFile.getAbsolutePath()
            );
            status.setText("Simulación completada.");
        } catch (Exception ex) {
            status.setText("Error: " + ex.getMessage());
        }
    }
}
