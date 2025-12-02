package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

import modules.gui.dashboard.*;

import modules.sync.SimulationEngine;

public class DashboardPage extends VBox {

    private ExePanel exePanel;
    private ProPanel proPanel;
    private MemPanel memPanel;
    private LogsPanel logsPanel;
    private ConfigPage configPage;
    private Button runButton;
    private Label statusLabel;

    //paso a paso
    private Button stepButton;
    private Button continueButton;
    private ToggleButton stepModeToggle;
    private boolean isStepMode = false;

    

    private SimulationEngine currentEngine;

    public DashboardPage() {
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("page-container");

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Simulación");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Configure los parámetros antes de iniciar");
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        
        // Botón de iniciar simulación
        runButton = new Button("Iniciar Simulación");
        runButton.getStyleClass().add("primary-button");
        runButton.setOnAction(e -> iniciarSimulacion());
        
        HBox toggleContainer = new HBox(8);
        toggleContainer.setAlignment(Pos.CENTER_LEFT);

        Label toggleLabel = new Label("Modo Paso a Paso:");
        toggleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        stepModeToggle = new ToggleButton();
        stepModeToggle.getStyleClass().add("toggle-button");
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("thumb");
        stepModeToggle.setGraphic(thumb);

        // Cambiar el listener para usar el toggle sin texto
        stepModeToggle.setOnAction(e -> {
            isStepMode = stepModeToggle.isSelected();
            statusLabel.setText(isStepMode ? 
                "Modo paso a paso activado" : 
                "Modo continuo activado");
        });

        toggleContainer.getChildren().addAll(toggleLabel, stepModeToggle);


        stepButton = new Button("Siguiente Paso →");
        stepButton.getStyleClass().add("secondary-button");
        stepButton.setDisable(true);
        stepButton.setOnAction(e -> avanzarPaso());

        continueButton = new Button("Continuar");
        continueButton.getStyleClass().add("primary-button");
        continueButton.setDisable(true);
        continueButton.setOnAction(e -> continuarSimulacion());


        topBar.getChildren().addAll(
                title,
                spacer,
                runButton,
                toggleContainer,
                stepButton,
                continueButton
        );

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 0, 0, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        grid.getRowConstraints().addAll(row1, row2);

        exePanel = new ExePanel();
        proPanel = new ProPanel();
        memPanel = new MemPanel(); // sin config


        logsPanel = new LogsPanel();

        grid.add(exePanel, 0, 0);
        grid.add(proPanel, 1, 0);
        grid.add(memPanel, 0, 1);
        grid.add(logsPanel, 1, 1);

        getChildren().addAll(topBar, grid);
      
        
    
    }

    // para conectar con ConfigPage (llamado desde MainFX)
    public void setConfigPage(ConfigPage configPage) {
        this.configPage = configPage;
        //System.out.println("[DashboardPage] ConfigPage conectado: " + configPage);
    }

    private void iniciarSimulacion() {
      if (configPage == null) {
          statusLabel.setText("Error: ConfigPage no conectado");
          statusLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 12px;");
          return;
      }
      
      System.out.println("[DashboardPage] Iniciando simulación...");
      
      statusLabel.setText("Simulación en curso...");
      statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
      memPanel.setConfig(configPage.getCurrentConfig());
      runButton.setDisable(true);
      
      configPage.setStepModeEnabled(isStepMode);

      // Llamar al método de ConfigPage para iniciar
      configPage.runSimulation();
      
      // DESPUÉS de iniciar, obtener el engine y configurar modo paso a paso
      new Thread(() -> {
        int attempts = 0;
        while (attempts < 10) { // Intentar hasta 10 veces (5 segundos)
            try {
                Thread.sleep(500);
                attempts++;
                
                javafx.application.Platform.runLater(() -> {
                    currentEngine = configPage.getCurrentEngine();
                    
                    if (currentEngine != null) {
                        if (isStepMode) {
                            stepButton.setDisable(false);
                            continueButton.setDisable(false);
                            statusLabel.setText("");
                        } else {
                            statusLabel.setText("");
                        }
                        runButton.setDisable(false);
                    }
                });
                
                if (currentEngine != null) {
                    break; // Salir del loop si ya tenemos el engine
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        
        if (currentEngine == null) {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Error: No se pudo obtener el engine");
            });
        }
    }).start();
  }

  private void avanzarPaso() {
    if (currentEngine != null && currentEngine.getSimulationController() != null) {
      currentEngine.getSimulationController().advanceOneStep();
    }
  }

  private void continuarSimulacion() {
    if (currentEngine != null && currentEngine.getSimulationController() != null) {
        currentEngine.getSimulationController().continueExecution();
        isStepMode = false;
        stepModeToggle.setSelected(false);
        stepButton.setDisable(true);
        continueButton.setDisable(true);
    }
  }

    // GETTERS PARA LOS PANELES
    public ProPanel getProPanel() {
        return proPanel;
    }

    public ExePanel getExePanel() {
        return exePanel;
    }

    public MemPanel getMemPanel() {
        return memPanel;
    }

    public LogsPanel getLogsPanel() {
        return logsPanel;
    }
}