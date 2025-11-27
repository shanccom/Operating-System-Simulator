package gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import gui.pages.ConfigPage;
import gui.pages.DashboardPage;
import gui.pages.ResultadosPage;

public class MainFX extends Application {

  //private File configFile = new File("src/main/resources/data/config.txt");
  //private File processFile = new File("src/main/resources/data/procesos.txt");
  private final Map<String, VBox> pages = new LinkedHashMap<>();
  @Override
  public void start(Stage stage) {
    stage.setTitle("Simulador de Sistema Operativo");

    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    crearPaginas(stage);
    //HBox navbar = crearNavbar();

    Scene scene = new Scene(root, 1000, 650);
    stage.setScene(scene);
    stage.show();
   
  }
  private void crearPaginas(Stage stage) {
    pages.put("configuracion", new ConfigPage(stage));
    pages.put("dashboard", new DashboardPage());
    pages.put("resultados", new ResultadosPage());
  }
  
  public static void main(String[] args) {
    launch(args);
  }
}