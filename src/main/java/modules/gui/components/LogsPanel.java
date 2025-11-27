package modules.gui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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

        list.setCellFactory(v -> new LogCell());

        getChildren().addAll(title, list);

        Logger.addListener(entry -> {
            Platform.runLater(() -> {
                list.getItems().add(entry);
                list.scrollTo(list.getItems().size() - 1);
            });
        });
    }

    private static class LogCell extends ListCell<Logger.LogEntry> {
        @Override
        protected void updateItem(Logger.LogEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText(null);
                setStyle("");
                return;
            }

            setText(item.toString());

            String color = switch (item.getLevel()) {
                case ERROR -> "#ff7070ff";
                case WARNING -> "#fac424ff";
                case DEBUG -> "#6b79a5ff";
                case EVENT -> "#15c98dff";
                default -> "#1f2937";
            };

            setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas';");
        }
    }
}
