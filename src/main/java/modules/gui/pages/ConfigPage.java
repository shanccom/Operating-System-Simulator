package modules.gui.pages;

import modules.gui.SimulationRunner;
import model.Config;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ConfigPage extends VBox {

    private File processFile = new File("src/main/resources/data/procesos.txt");

    private final Spinner<Integer> framesSpinner = new Spinner<>(1, 100, 3);
    private final Spinner<Integer> frameSizeSpinner = new Spinner<>(512, 8192, 4096, 512);
    private final ComboBox<String> schedulerCombo = new ComboBox<>();
    private final Spinner<Integer> quantumSpinner = new Spinner<>(1, 20, 2);
    private final ComboBox<String> replacementCombo = new ComboBox<>();
    private final CheckBox enableIOCheck = new CheckBox("Habilitar I/O");
    private final Spinner<Integer> timeUnitSpinner = new Spinner<>(10, 1000, 100, 10);

    private final Label labelProcess = new Label();
    private final Label labelStatus = new Label();

    private Config currentConfig;
    private DashboardPage dashboardPage;


    private VBox buildConsiderationsCard() {
        VBox mainBox = new VBox(15);
        mainBox.getStyleClass().add("card");
        mainBox.setMaxWidth(Double.MAX_VALUE);
        mainBox.setPadding(new Insets(20));

        // Título principal
        Label mainTitle = new Label("Simulador de Sistema Operativo");
        mainTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: rgba(240, 225, 255, 0.95);");
        
        Label subtitle = new Label("Administración de memoria y procesos en tiempo real");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(240, 225, 255, 0.7);");

        // Contenedor de las tres columnas
        HBox modulesContainer = new HBox(15);
        modulesContainer.setAlignment(Pos.CENTER);
        modulesContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);


        VBox memoryModule = createModuleCard(
            "Memoria",
            "module-memory",
            "• Direccionamiento virtual (páginas)\n" +
            "• Traducción a frames físicos\n" +
            "• Page faults automáticos\n" +
            "• Algoritmos: FIFO, LRU, OPTIMAL, NRU"
        );


        VBox processModule = createModuleCard(
            "Procesos",
            "module-process",
            "• Estados: Listo, Ejecutando, Bloqueado\n" +
            "• Planificadores: FCFS, SJF, RR, Prioridad\n" +
            "• Soporte para I/O\n" +
            "• Gestión de quantum (RR)"
        );


        VBox executionModule = createModuleCard(
            "Ejecución",
            "module-execution",
            "• Diagrama de Gantt en tiempo real\n" +
            "• Visualización del orden de ejecución\n" +
            "• Interrupciones preemptivas\n" +
            "• Métricas de rendimiento"
        );

        // Distribucion
        HBox.setHgrow(executionModule, Priority.ALWAYS);
        HBox.setHgrow(processModule, Priority.ALWAYS);
        HBox.setHgrow(memoryModule, Priority.ALWAYS);
        

        modulesContainer.getChildren().addAll(memoryModule, processModule, executionModule);

        mainBox.getChildren().addAll(mainTitle, subtitle, modulesContainer);
        return mainBox;
    }

    private VBox createModuleCard(String title, String styleClass, String content) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("module-card", styleClass);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("module-title");

        // Contenido
        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("module-content");
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(titleLabel, contentLabel);
        return card;
    }




    private VBox wrapInConfigCard(GridPane configGrid) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(15));
        box.getChildren().add(configGrid);
        return box;
    }



    public ConfigPage(Stage stage, DashboardPage dashboardPage) {
        this.dashboardPage = dashboardPage;

        setSpacing(0);
        setPadding(new Insets(0));
        setAlignment(Pos.TOP_CENTER);
        getStyleClass().add("page-container");

        currentConfig = new Config();

        labelProcess.setText("Archivo: " + processFile.getName());
        labelProcess.getStyleClass().add("text-clear");

        labelStatus.setText("Visualiza el comportamiento interno de un SO");
        labelStatus.getStyleClass().add("status-text");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);


        // --- Construcción de secciones ---
        VBox considerationsCard = buildConsiderationsCard();
        VBox sectionFile = buildFileSection(stage);

        GridPane configGrid = buildConfigGrid();
        VBox configCard = wrapInConfigCard(configGrid);   // <-- Envolvemos

        Button startButton = buildStartButton();


        // --- Orden final de los elementos ---
        content.getChildren().addAll(
            considerationsCard,
            sectionFile,
            configCard,
            startButton,
            labelStatus
        );

        scrollPane.setContent(content);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }


    /* ------------------------------------------
     *  CARD: Archivo de Procesos
     * ------------------------------------------ */
    private VBox buildFileSection(Stage stage) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Archivo de Procesos");
        title.getStyleClass().add("card-title");

        HBox rowProcess = new HBox(10);
        rowProcess.setAlignment(Pos.CENTER_LEFT);

        Button btnProcess = new Button("Cargar Archivo");
        btnProcess.getStyleClass().add("secondary-button");

        btnProcess.setOnAction(e -> {
            File f = openFile(stage);
            if (f != null) {
                processFile = f;
                labelProcess.setText("Archivo: " + f.getName());
                labelStatus.setText("Archivo cargado. Listo para iniciar.");
            }
        });

        rowProcess.getChildren().addAll(labelProcess, btnProcess);
        box.getChildren().addAll(title, rowProcess);

        return box;
    }

    private GridPane buildConfigGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);

        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        Label mainTitle = new Label("Configuración del Sistema");
        mainTitle.getStyleClass().add("card-title");
        grid.add(mainTitle, 0, row++, 2, 1);


        // ------ Memoria ------
        VBox leftSection = new VBox(10);
        leftSection.getStyleClass().add("config-section");

        Label memTitle = new Label("Memoria");
        memTitle.getStyleClass().add("section-title");
        memTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        framesSpinner.setEditable(true);
        framesSpinner.setPrefWidth(80);

        frameSizeSpinner.setEditable(true);
        frameSizeSpinner.setPrefWidth(80);

        replacementCombo.getItems().addAll("FIFO", "LRU", "OPTIMAL", "NRU");
        replacementCombo.getSelectionModel().select("LRU");
        replacementCombo.setPrefWidth(120);

        leftSection.getChildren().addAll(
            memTitle,
            createCompactRow("Frames:", framesSpinner),
            createCompactRow("Tamaño (bytes):", frameSizeSpinner),
            createCompactRow("Reemplazo:", replacementCombo)
        );

        grid.add(leftSection, 0, row, 1, 1);


        // ------ CPU ------
        VBox rightSection = new VBox(10);
        rightSection.getStyleClass().add("config-section");

        Label cpuTitle = new Label("CPU");
        cpuTitle.getStyleClass().add("section-title");
        cpuTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        schedulerCombo.getItems().addAll("FCFS", "SJF", "RR", "PRIORITY");
        schedulerCombo.getSelectionModel().select("FCFS");
        schedulerCombo.setPrefWidth(120);

        quantumSpinner.setEditable(true);
        quantumSpinner.setPrefWidth(80);
        quantumSpinner.setDisable(true);

        schedulerCombo.setOnAction(e -> {
            quantumSpinner.setDisable(!schedulerCombo.getValue().equals("RR"));
        });

        rightSection.getChildren().addAll(
            cpuTitle,
            createCompactRow("Scheduler:", schedulerCombo),
            createCompactRow("Quantum:", quantumSpinner)
        );

        grid.add(rightSection, 1, row++, 1, 1);



        VBox advancedSection = new VBox(8);

        Label advTitle = new Label("Configuración Avanzada");
        advTitle.getStyleClass().add("section-title");
        advTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        enableIOCheck.setSelected(false);

        timeUnitSpinner.setEditable(true);
        timeUnitSpinner.setPrefWidth(80);

        HBox advancedRow = new HBox(20);
        advancedRow.setAlignment(Pos.CENTER_LEFT);
        advancedRow.getChildren().addAll(
            enableIOCheck,
            createCompactRow("Tiempo (ms):", timeUnitSpinner)
        );

        advancedSection.getChildren().addAll(advTitle, advancedRow);
        grid.add(advancedSection, 0, row++, 2, 1);

        return grid;
    }


    private HBox createCompactRow(String labelText, Control control) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.getStyleClass().add("text-clear");
        label.setMinWidth(100);
        label.setStyle("-fx-font-size: 13px;");

        row.getChildren().addAll(label, control);
        return row;
    }


    private Button buildStartButton() {
        Button btn = new Button("Visualize");
        btn.getStyleClass().add("primary-button");
        btn.setOnAction(e -> runSimulation());
        btn.setMaxWidth(400);
        btn.setPrefHeight(40);
        return btn;
    }


    private File openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files", "*.txt"));

        File initialDir = new File("src/main/resources/data");
        if (initialDir.exists()) {
            fc.setInitialDirectory(initialDir);
        }

        return fc.showOpenDialog(stage);
    }


    private Config buildConfigFromForm() {
        Config config = new Config();

        try {
            config.setTotalFrames(framesSpinner.getValue());
            config.setFrameSize(frameSizeSpinner.getValue());
            config.setReplacementType(parseReplacementType(replacementCombo.getValue()));
            config.setSchedulerType(parseSchedulerType(schedulerCombo.getValue()));
            config.setQuantum(quantumSpinner.getValue());
            config.setTimeUnit(timeUnitSpinner.getValue());

        } catch (Exception e) {
            labelStatus.setText("Error al construir configuración: " + e.getMessage());
            return null;
        }

        return config;
    }


    private Config.SchedulerType parseSchedulerType(String value) {
        return switch (value.toUpperCase()) {
            case "FCFS" -> Config.SchedulerType.FCFS;
            case "SJF" -> Config.SchedulerType.SJF;
            case "RR" -> Config.SchedulerType.ROUND_ROBIN;
            case "PRIORITY" -> Config.SchedulerType.PRIORITY;
            default -> Config.SchedulerType.FCFS;
        };
    }


    private Config.ReplacementType parseReplacementType(String value) {
        return switch (value.toUpperCase()) {
            case "FIFO" -> Config.ReplacementType.FIFO;
            case "LRU" -> Config.ReplacementType.LRU;
            case "OPTIMAL" -> Config.ReplacementType.OPTIMAL;
            case "NRU" -> Config.ReplacementType.NRU;
            default -> Config.ReplacementType.FIFO;
        };
    }


    public void runSimulation() {
        try {
            if (processFile == null || !processFile.exists()) {
                labelStatus.setText("Error: Debe cargar un archivo de procesos.");
                return;
            }

            currentConfig = buildConfigFromForm();
            if (currentConfig == null) return;

            if (!currentConfig.validate()) {
                labelStatus.setText("Error: Configuración inválida.");
                return;
            }

            labelStatus.setText("Iniciando simulación...");

            SimulationRunner.runSimulation(
                currentConfig,
                processFile.getAbsolutePath(),
                dashboardPage != null ? dashboardPage.getProPanel() : null,
                dashboardPage.getMemPanel()
            );

            labelStatus.setText("Simulación iniciada correctamente.");

        } catch (Exception ex) {
            labelStatus.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    public Config getCurrentConfig() {
        return buildConfigFromForm();
    }
}
