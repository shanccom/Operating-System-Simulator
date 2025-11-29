package modules.gui.dashboard;

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


        Label title = new Label("Logs en Tiempo Real");
        title.getStyleClass().add("card-title");

        list.setCellFactory(v -> new LogCell());

        list.getStyleClass().add("logs-panel");

        getChildren().addAll(title, list);

        Logger.addListener(this::addLog);
    }

    private void addLog(Logger.LogEntry entry) {
        Platform.runLater(() -> {
            list.getItems().add(entry);
            list.scrollTo(list.getItems().size() - 1);
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
                case ERROR -> "#ef4444";
                case WARNING -> "#f59e0b";
                case DEBUG -> "#e0e9fbff";
                case EVENT -> "#10b981";
                default -> "#9fa5acff";
            };

            setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas';" + "-fx-background-color: #0f0a1a; " + "-fx-font-size: 13px;");
        }
    }
}
