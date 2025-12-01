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
        setSpacing(9);
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
                case MEM -> "#ff5edfff";
                case EXE -> "#9d94fdff";
                case PROC -> "#9bfdcaff";
                case SYNC -> "#fff7beff";
                case ERROR -> "#fd0000ff";
                case WARNING -> "#fd7600ff";
                default -> "#ffffffff";
            };

            setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas';" + "-fx-background-color: #0f0a1a; " + "-fx-font-size: 13.5px;");
            setStyle("-fx-text-fill: " + color + ";" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +  // Grosor medio válido
            "-fx-background-color: #0f0a1a;" +
            "-fx-padding: 6px;" +
            "-fx-border-color: #1a1a1a;" +
            "-fx-border-width: 1px;");
        }
    }
}
