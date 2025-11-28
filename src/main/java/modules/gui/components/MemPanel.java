package modules.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
public class MemPanel  extends VBox {

    public MemPanel() {
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("card");

        Label title = new Label("Panel de Ejecución");
        title.getStyleClass().add("card-title");
        getChildren().add(title);
    }
}
