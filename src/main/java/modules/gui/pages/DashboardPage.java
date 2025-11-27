package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

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


        topBar.getChildren().addAll(
                title,
                subtitle,
                spacer
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
        VBox execPanel = buildExePanel("Panel de Ejecución");
        grid.add(execPanel, 0, 0);

        VBox queuesPanel = buildProPanel("Colas de Procesos");
        grid.add(queuesPanel, 1, 0);

        VBox memoryPanel = buildMemPanel("Panel de Memoria Virtual");
        grid.add(memoryPanel, 0, 1);

        VBox logsPanel = buildLogsPanel("Logs en Tiempo Real");
        grid.add(logsPanel, 1, 1);

        getChildren().addAll(topBar, grid);
    }

    private VBox buildExePanel(String titleText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        card.getChildren().add(title);

        return card;
    }

    private VBox buildLogsPanel(String titleText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        card.getChildren().add(title);

        return card;
    }
    private VBox buildMemPanel(String titleText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        card.getChildren().add(title);

        return card;
    }
    private VBox buildProPanel(String titleText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        card.getChildren().add(title);

        return card;
    }
}
