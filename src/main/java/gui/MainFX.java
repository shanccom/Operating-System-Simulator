package gui;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;

public class MainFX extends Application {

    private File configFile;
    private File processFile;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Simulador de Sistema Operativo");

        Label label = new Label("Selecciona archivos para iniciar la simulación");
        Button btnConfig = new Button("Seleccionar archivo de configuración");
        Button btnProcess = new Button("Seleccionar archivo de procesos");
        Button btnRun = new Button("Iniciar simulación");
        btnConfig.setOnAction(e -> {
            configFile = openFile(stage);
        });

        btnProcess.setOnAction(e -> {
            processFile = openFile(stage);
        });

        btnRun.setOnAction(e -> {
            if (configFile == null || processFile == null) {
                label.setText("Selecciona ambos archivos antes de iniciar.");
                return;
            }

            try {
                gui.SimulationRunner.runSimulation(
                        configFile.getAbsolutePath(),
                        processFile.getAbsolutePath()
                );
                label.setText("Simulación completada (ver consola)");
                // 🔥 Aquí abrimos la ventana de memoria
                Stage memoryStage = new Stage();
                gui.MemoryVisualizer visualizer = new gui.MemoryVisualizer();
                visualizer.start(memoryStage);




            } catch (Exception ex) {
                label.setText("Error al ejecutar la simulación: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        VBox root = new VBox(10, label, btnConfig, btnProcess, btnRun);
        root.setStyle("-fx-padding: 20; -fx-font-size: 14px;");
        stage.setScene(new Scene(root, 400, 250));
        stage.show();
    }

    private File openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        return fc.showOpenDialog(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
