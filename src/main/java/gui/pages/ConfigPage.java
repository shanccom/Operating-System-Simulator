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

        Label title = new Label("Load Input Files");
        Label subtitle = new Label("Selecciona archivos de configuración y procesos.");

        Button btnConfig = new Button("Cargar archivo de configuración");
        Button btnProcess = new Button("Cargar archivo de procesos");

        btnConfig.setOnAction(e -> {
            File f = openFile(stage);
            if (f != null) {
                configFile = f;
                labelConfig.setText("Config: " + f.getName());
            }
        });

        btnProcess.setOnAction(e -> {
            File f = openFile(stage);
            if (f != null) {
                processFile = f;
                labelProcess.setText("Procesos: " + f.getName());
            }
        });

        box.getChildren().addAll(
                title,
                subtitle,
                new HBox(10, labelConfig, btnConfig),
                new HBox(10, labelProcess, btnProcess)
        );

        return box;
    }

    private VBox buildCpuSection() {
        VBox box = new VBox(10);

        Label title = new Label("Configuración de Algoritmo de Planificación CPU");
        Label subtitle = new Label("Selecciona el algoritmo para la simulación.");

        ComboBox<String> schedulerCombo = new ComboBox<>();
        schedulerCombo.getItems().addAll("FCFS", "RR", "SJF");
        schedulerCombo.getSelectionModel().select("FCFS");

        box.getChildren().addAll(
                title,
                subtitle,
                new HBox(10, new Label("Scheduler:"), schedulerCombo)
        );

        return box;
    }

    private VBox buildMemorySection() {
        VBox box = new VBox(10);

        Label title = new Label("Configuración de Memoria");
        Label subtitle = new Label("Selecciona el algoritmo de reemplazo.");

        ComboBox<String> replaceCombo = new ComboBox<>();
        replaceCombo.getItems().addAll("FIFO", "LRU", "OPTIMAL");
        replaceCombo.getSelectionModel().select("FIFO");

        box.getChildren().addAll(
                title,
                subtitle,
                new HBox(10, new Label("Replacement:"), replaceCombo)
        );

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
