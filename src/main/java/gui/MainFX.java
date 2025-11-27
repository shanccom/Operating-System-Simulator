package gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

  private final Map<String, VBox> pages = new LinkedHashMap<>();
  private final Map<String, Button> navButtons = new LinkedHashMap<>();
  @Override
  public void start(Stage stage) {
    stage.setTitle("Simulador de Sistema Operativo");

    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    crearPaginas(stage);
    HBox nbar = crearNavbar();
    root.setTop(nbar);
    root.setCenter(pages.get("config"));

    Scene scene = new Scene(root, 1000, 650);
    stage.setScene(scene);
    stage.show();
   
  }
  private void crearPaginas(Stage stage) {
    pages.put("configuracion", new ConfigPage(stage));
    pages.put("dashboard", new DashboardPage());
    pages.put("resultados", new ResultadosPage());
  }
  private HBox crearNavbar() {
    HBox navbar = new HBox(10);

    Button configBtn = crearNavButton("Configuracion", "config");
    Button dashboardBtn = crearNavButton("Dashboard", "dashboard");
    Button resultsBtn = crearNavButton("Resultados", "results");

    navbar.getChildren().addAll(
      configBtn,
      dashboardBtn,
      resultsBtn
    );

    return navbar;
  }

  private Button crearNavButton(String text, String pageKey) {
    Button button = new Button(text);
    button.setOnAction(e -> switchPage(pageKey));
    navButtons.put(pageKey, button);
    return button;
  }

  private void switchPage(String pageKey) {
    VBox page = pages.get(pageKey);
    if (page != null) {
      BorderPane root = (BorderPane) navButtons
        .values()
        .iterator()
        .next()
        .getScene()
        .getRoot();
      root.setCenter(page);
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}