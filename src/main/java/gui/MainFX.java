package gui;

import gui.pages.ConfigPage;
import gui.pages.DashboardPage;
import gui.pages.ResultadosPage;

import java.util.LinkedHashMap;
import java.util.Map;

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

public class MainFX extends Application {

    private final Map<String, VBox> pages = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(16));

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
        stage.show();
    }

    private void crearPaginas(Stage stage) {
        pages.put("config", new ConfigPage(stage));
        pages.put("dashboard", new DashboardPage());
        pages.put("resultados", new ResultadosPage());
    }

    private HBox crearNavbar() {
        HBox navbar = new HBox(16);
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setPadding(new Insets(14, 16, 14, 16));

        Label title = new Label("Sistema Operativo");
        title.getStyleClass().add("brand");

        Button configBtn = crearNavButton("Configuración", "config");
        Button dashboardBtn = crearNavButton("Dashboard", "dashboard");
        Button resultsBtn = crearNavButton("Resultados", "resultados");

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

    public static void main(String[] args) {
        launch(args);
    }
}
