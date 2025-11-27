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
        labelStatus.getStyleClass().add("status-text");

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
        VBox box = new VBox(12);
        box.getStyleClass().add("card");

        Label title = new Label("Load Input Files");
        title.getStyleClass().add("card-title");

        Label subtitle = new Label("Selecciona archivos de configuración y procesos.");
        subtitle.getStyleClass().add("card-subtitle");

        HBox rowConfig = new HBox(10);
        rowConfig.setAlignment(Pos.CENTER_LEFT);

        Button btnConfig = new Button("Cargar archivo de configuración");
        btnConfig.getStyleClass().add("secondary-button");

        btnConfig.setOnAction(e -> {
            File f = openFile(stage);
            if (f != null) {
                configFile = f;
                labelConfig.setText("Config: " + f.getName());
            }
        });

        HBox rowProcess = new HBox(10);
        rowProcess.setAlignment(Pos.CENTER_LEFT);

        Button btnProcess = new Button("Cargar archivo de procesos");
        btnProcess.getStyleClass().add("secondary-button");

        btnProcess.setOnAction(e -> {
            File f = openFile(stage);
            if (f != null) {
                processFile = f;
                labelProcess.setText("Procesos: " + f.getName());
            }
        });

        rowConfig.getChildren().addAll(labelConfig, btnConfig);
        rowProcess.getChildren().addAll(labelProcess, btnProcess);

        box.getChildren().addAll(title, subtitle, rowConfig, rowProcess);
        return box;
    }

    private VBox buildCpuSection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");

        Label title = new Label("Configuración de Algoritmo de Planificación CPU");
        title.getStyleClass().add("card-title");

        Label subtitle = new Label("Selecciona el algoritmo para la simulación.");
        subtitle.getStyleClass().add("card-subtitle");

        ComboBox<String> schedulerCombo = new ComboBox<>();
        schedulerCombo.getItems().addAll("FCFS", "RR", "SJF");
        schedulerCombo.getSelectionModel().select("FCFS");
        schedulerCombo.getStyleClass().add("input-control");

        HBox row = new HBox(10, new Label("Scheduler:"), schedulerCombo);
        row.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(title, subtitle, row);
        return box;
    }

    private VBox buildMemorySection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");

        Label title = new Label("Configuración de Memoria");
        title.getStyleClass().add("card-title");

        Label subtitle = new Label("Selecciona el algoritmo de reemplazo.");
        subtitle.getStyleClass().add("card-subtitle");

        ComboBox<String> replaceCombo = new ComboBox<>();
        replaceCombo.getItems().addAll("FIFO", "LRU", "OPTIMAL");
        replaceCombo.getSelectionModel().select("FIFO");
        replaceCombo.getStyleClass().add("input-control");

        HBox row = new HBox(10, new Label("Replacement:"), replaceCombo);
        row.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(title, subtitle, row);
        return box;
    }

    private Button buildStartButton() {
        Button btn = new Button("Start Simulation");
        btn.getStyleClass().add("primary-button");
        btn.setOnAction(e -> runSimulation(labelStatus));
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private File openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("TXT files", "*.txt")
        );

        File initialDir = new File("src/main/resources/data");
        if (initialDir.exists()) {
            fc.setInitialDirectory(initialDir);
        }
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
