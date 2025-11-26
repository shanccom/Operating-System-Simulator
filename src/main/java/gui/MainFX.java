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

  private File configFile = new File("src/main/resources/data/config.txt");
  private File processFile = new File("src/main/resources/data/procesos.txt");

  @Override
  public void start(Stage stage) {
    stage.setTitle("Simulador de Sistema Operativo");

    Label labelConfig = new Label("Config: " + configFile.getName());
    Label labelProcess = new Label("Procesos: " + processFile.getName());
    Label labelStatus = new Label("Archivos cargados. Presiona 'Iniciar' para comenzar.");
    
    Button btnConfig = new Button("Cambiar archivo de configuración");
    Button btnProcess = new Button("Cambiar archivo de procesos");
    Button btnRun = new Button("Iniciar simulación");

    btnConfig.setOnAction(e -> {
      File newFile = openFile(stage);
      if (newFile != null) {
        configFile = newFile;
        labelConfig.setText("Config: " + configFile.getName());
        labelStatus.setText("Configuración actualizada");
      }
    });

    btnProcess.setOnAction(e -> {
      File newFile = openFile(stage);
      if (newFile != null) {
        processFile = newFile;
        labelProcess.setText("Procesos: " + processFile.getName());
        labelStatus.setText("Archivo de procesos actualizado");
      }
    });

    btnRun.setOnAction(e -> {
      if (!configFile.exists() || !processFile.exists()) {
        labelStatus.setText("Error: Los archivos no existen en las rutas especificadas");
        return;
      }

      try {
        labelStatus.setText("Ejecutando simulación...");
        
        SimulationRunner.runSimulation(
          configFile.getAbsolutePath(),
          processFile.getAbsolutePath()
        );
        
        labelStatus.setText("Simulación completada (ver consola)");
          
      } catch (Exception ex) {
        labelStatus.setText("Error: " + ex.getMessage());
        ex.printStackTrace();
      }
    });

    VBox root = new VBox(10, labelConfig, labelProcess, labelStatus, 
                         btnConfig, btnProcess, btnRun);
    root.setStyle("-fx-padding: 20; -fx-font-size: 14px;");
    stage.setScene(new Scene(root, 900, 600));

    stage.show();
  }

  private File openFile(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.getExtensionFilters().addAll(
      new FileChooser.ExtensionFilter("Archivos de texto", "*.txt"),
      new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
    );
    return fc.showOpenDialog(stage);
  }

  public static void main(String[] args) {
    launch(args);
  }
}