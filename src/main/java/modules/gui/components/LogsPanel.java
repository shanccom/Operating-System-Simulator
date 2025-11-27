package modules.gui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import utils.Logger;

public class LogsPanel extends VBox {

    private final ListView<Logger.LogEntry> list = new ListView<>();

    public LogsPanel() {
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("card");

        Label title = new Label("Panel de Ejecución");
        title.getStyleClass().add("card-title");

        getChildren().addAll(title, list);

        Logger.addListener(entry -> {
            list.getItems().add(entry);   
            list.scrollTo(list.getItems().size() - 1);
        });
    }
}
