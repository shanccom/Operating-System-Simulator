package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.control.ToggleButton;
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
    private Button stepButtonExe;
    private Button continueButtonExe;
    private ToggleButton stepModeToggleExe;
    private boolean isStepModeExe = false;

    // Para manejar la expansión
    private GridPane grid;
    private HBox topBar;

    private SimulationEngine currentEngine;

    public DashboardPage() {
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("page-container");

        topBar = new HBox(10);
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

        stepModeToggleExe = new ToggleButton();
        stepModeToggleExe.getStyleClass().add("toggle-button");
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("thumb");
        stepModeToggleExe.setGraphic(thumb);

        // Cambiar el listener para usar el toggle sin texto
        stepModeToggleExe.setOnAction(e -> {
            isStepModeExe = stepModeToggleExe.isSelected();
            statusLabel.setText(isStepModeExe ? 
                "Modo paso a paso activado" : 
                "Modo continuo activado");
        });

        toggleContainer.getChildren().addAll(toggleLabel, stepModeToggleExe);


        stepButtonExe = new Button("Siguiente Paso →");
        stepButtonExe.getStyleClass().add("secondary-button");
        stepButtonExe.setDisable(true);
        stepButtonExe.setOnAction(e -> avanzarPaso());

        continueButtonExe = new Button("Continuar");
        continueButtonExe.getStyleClass().add("primary-button");
        continueButtonExe.setDisable(true);
        continueButtonExe.setOnAction(e -> continuarSimulacion());


        topBar.getChildren().addAll(
                title,
                spacer,
                runButton,
                toggleContainer,
                stepButtonExe,
                continueButtonExe
        );

        grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 0, 0, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(70);
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

        // configurar los callbacks de expansión
        exePanel.setOnExpand(() -> expandExePanel());
        exePanel.setOnCollapse(() -> collapseExePanel());

        grid.add(exePanel, 0, 0);
        grid.add(proPanel, 1, 0);
        grid.add(memPanel, 0, 1);
        grid.add(logsPanel, 1, 1);

        getChildren().addAll(topBar, grid);
    }
    
    private void expandExePanel() {
        // Ocultar los otros paneles
        proPanel.setVisible(false);
        proPanel.setManaged(false);
        memPanel.setVisible(false);
        memPanel.setManaged(false);
        logsPanel.setVisible(false);
        logsPanel.setManaged(false);

        // Hacer que ExePanel ocupe toda la grilla
        GridPane.setColumnSpan(exePanel, 2);
        GridPane.setRowSpan(exePanel, 2);

        // Aumentar el tamaño del panel
        VBox.setVgrow(grid, Priority.ALWAYS);
    }
    //Restaurar todo
    private void collapseExePanel() {
        // Restaurar la visibilidad de los otros paneles
        proPanel.setVisible(true);
        proPanel.setManaged(true);
        memPanel.setVisible(true);
        memPanel.setManaged(true);
        logsPanel.setVisible(true);
        logsPanel.setManaged(true);

        // Restaurar el span original de ExePanel
        GridPane.setColumnSpan(exePanel, 1);
        GridPane.setRowSpan(exePanel, 1);

        // Restaurar el tamaño del grid
        VBox.setVgrow(grid, Priority.SOMETIMES);
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
      
      configPage.setStepModeEnabled(isStepModeExe);

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
                        if (isStepModeExe) {
                            stepButtonExe.setDisable(false);
                            continueButtonExe.setDisable(false);
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
        isStepModeExe = false;
        stepModeToggleExe.setSelected(false);
        stepButtonExe.setDisable(true);
        continueButtonExe.setDisable(true);
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