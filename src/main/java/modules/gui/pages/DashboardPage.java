package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import modules.gui.MainFX;
import modules.gui.SimulationRunner;
import modules.gui.pages.ConfigPage;
import modules.gui.components.*;
import utils.Logger;
public class DashboardPage extends VBox {

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
        
        Button clearLogButton = new Button("Limpiar Log");
        clearLogButton.getStyleClass().add("secondary-button");

        Button runButton = new Button("Iniciar Simulación");
        runButton.getStyleClass().add("primary-button");

        ConfigPage cp = new ConfigPage(null);

        runButton.setOnAction(e -> {

            cp.runSimulation();
        });
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

        //Paneles
        grid.add(new ExePanel(), 0, 0);
        grid.add(new ProPanel(), 1, 0);
        grid.add(new MemPanel(), 0, 1);
        grid.add(new LogsPanel(), 1, 1);

        getChildren().addAll(topBar, grid);
    }

}
