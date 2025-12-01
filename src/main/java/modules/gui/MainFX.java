package modules.gui;


import java.util.LinkedHashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.DatosResultados;
import modules.gui.pages.ConfigPage;
import modules.gui.pages.DashboardPage;
import modules.gui.pages.ResultadosPage;

public class MainFX extends Application {

    private final Map<String, VBox> pages = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();


    private DashboardPage dashboardPage;
    private ConfigPage configPage;

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        crearPaginas(stage);
        HBox navbar = crearNavbar();

        root.setTop(navbar);
        root.setCenter(pages.get("config"));


        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(
            getClass().getResource("/gui/styles.css").toExternalForm()
        );

        stage.setTitle("Simulador de Sistema Operativo");
        
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    private void crearPaginas(Stage stage) {
        dashboardPage = new DashboardPage();
        
        configPage = new ConfigPage(stage, dashboardPage, this);
        dashboardPage.setConfigPage(configPage);
        
        pages.put("config", configPage);
        pages.put("dashboard", dashboardPage);
        pages.put("resultados", new ResultadosPage());
        //deubug
        //System.out.println("[MainFX] DashboardPage creado: " + dashboardPage);
        //System.out.println("[MainFX] ConfigPage creado: " + configPage);
        //System.out.println("[MainFX] ProPanel disponible: " + dashboardPage.getProPanel());
    }

    private HBox crearNavbar() {
        HBox navbar = new HBox(16);
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setPadding(new Insets(14, 16, 14, 16));

        Label title = new Label("OS Scheduling Algorithm Visualizer");
        title.getStyleClass().add("brand");
        

        Button configBtn = crearNavButton("Inicio", "config");
        Button dashboardBtn = crearNavButton("Visualización", "dashboard");
        Button resultsBtn = crearNavButton("Metricas", "resultados");

        HBox navButtonsRow = new HBox(8, configBtn, dashboardBtn, resultsBtn);
        navButtonsRow.setAlignment(Pos.CENTER_LEFT);

        navbar.getChildren().addAll(title, navButtonsRow);

        activarBoton("config");
        return navbar;
    }

    private Button crearNavButton(String text, String pageKey) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setOnAction(e -> switchPage(pageKey));
        navButtons.put(pageKey, button);
        return button;
    }

    private void activarBoton(String activeKey) {
        navButtons.forEach((key, button) -> {
            button.getStyleClass().remove("nav-button-active");
            if (key.equals(activeKey)) {
                button.getStyleClass().add("nav-button-active");
            }
        });
    }

    private void switchPage(String key) {
        BorderPane root = (BorderPane) navButtons
                .values().iterator().next()
                .getScene().getRoot();

        root.setCenter(pages.get(key));
        activarBoton(key);
    }

    public void showResultados(ResultadosPage nResultados) {
        pages.put("resultados", nResultados);
        switchPage("resultados");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
