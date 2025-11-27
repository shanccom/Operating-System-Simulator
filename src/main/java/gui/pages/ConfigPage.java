package gui.pages;

import gui.SimulationRunner;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ConfigPage extends VBox {

  private File configFile = new File("src/main/resources/data/config.txt");
  private File processFile = new File("src/main/resources/data/procesos.txt");

  public ConfigPage(Stage stage) {
    setSpacing(14);
    setPadding(new Insets(20));
    setAlignment(Pos.TOP_LEFT);
    getStyleClass().add("page-container");

    Label title = new Label("Configuración");
    title.getStyleClass().add("page-title");

    Label description = new Label(
      "Selecciona los archivos de configuración y procesos para iniciar la simulación."
    );
    description.getStyleClass().add("page-subtitle");

    Label labelConfig = new Label("Config: " + configFile.getName());
    Label labelProcess = new Label("Procesos: " + processFile.getName());
    Label labelStatus = new Label(
      "Archivos cargados. Presiona 'Iniciar simulación' para comenzar."
    );

    Button btnConfig = new Button("Cambiar archivo de configuración");
    Button btnProcess = new Button("Cambiar archivo de procesos");
    Button btnRun = new Button("Iniciar simulación");

    btnConfig.getStyleClass().add("secondary-button");
    btnProcess.getStyleClass().add("secondary-button");
    btnRun.getStyleClass().add("primary-button");

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

    btnRun.setOnAction(e -> runSimulation(labelStatus));

    HBox filesRow = new HBox(12, labelConfig, labelProcess);
    filesRow.setAlignment(Pos.CENTER_LEFT);

    getChildren().addAll(
      title,
      description,
      new Separator(),
      filesRow,
      new HBox(12, btnConfig, btnProcess),
      btnRun,
      labelStatus
    );
  }

  private void runSimulation(Label labelStatus) {
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
  }

  private File openFile(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.getExtensionFilters().addAll(
      new FileChooser.ExtensionFilter("Archivos de texto", "*.txt"),
      new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
    );
    return fc.showOpenDialog(stage);
  }
}
