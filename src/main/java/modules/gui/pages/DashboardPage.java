package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import modules.gui.SimulationRunner;
import modules.gui.pages.ConfigPage;
import modules.gui.dashboard.*;

public class DashboardPage extends VBox {

    // ✅ HACER LOS PANELES ATRIBUTOS DE LA CLASE
    private ExePanel exePanel;
    private ProPanel proPanel;
    private MemPanel memPanel;
    private LogsPanel logsPanel;
    private ConfigPage configPage;
    private Button runButton;
    private Label statusLabel;

    public DashboardPage() {
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("page-container");

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Simulacion");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Dashboard");
        subtitle.getStyleClass().add("page-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Configure los parámetros antes de iniciar");
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        
        // ✅ Botón de iniciar simulación
        runButton = new Button("Iniciar Simulación");
        runButton.getStyleClass().add("primary-button");
        runButton.setOnAction(e -> iniciarSimulacion());


        
        topBar.getChildren().addAll(
                title,
                subtitle,
                spacer,
                runButton
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

        // ✅ INSTANCIAR PANELES COMO ATRIBUTOS
        exePanel = new ExePanel();
        proPanel = new ProPanel();
        memPanel = new MemPanel();
        logsPanel = new LogsPanel();

        grid.add(exePanel, 0, 0);
        grid.add(proPanel, 1, 0);
        grid.add(memPanel, 0, 1);
        grid.add(logsPanel, 1, 1);

        getChildren().addAll(topBar, grid);
    }

    // ✅ MÉTODO para conectar con ConfigPage (llamado desde MainFX)
    public void setConfigPage(ConfigPage configPage) {
        this.configPage = configPage;
        System.out.println("[DashboardPage] ✅ ConfigPage conectado: " + configPage);
    }

    // ✅ MÉTODO para iniciar la simulación
    private void iniciarSimulacion() {
        if (configPage == null) {
            statusLabel.setText("⚠️ Error: ConfigPage no conectado");
            statusLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 12px;");
            System.out.println("[DashboardPage] ❌ ConfigPage es null");
            return;
        }
        
        System.out.println("[DashboardPage] 🚀 Iniciando simulación...");
        statusLabel.setText("🚀 Simulación en curso...");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
        
        runButton.setDisable(true);
        
        // Llamar al método de ConfigPage para iniciar
        configPage.runSimulation();
        
        // Opcional: Re-habilitar después de un tiempo
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    runButton.setDisable(false);
                    statusLabel.setText("✅ Simulación iniciada correctamente");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    

    // ✅ GETTERS PARA LOS PANELES
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