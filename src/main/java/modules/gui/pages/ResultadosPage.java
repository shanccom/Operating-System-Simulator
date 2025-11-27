package modules.gui.pages;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ResultadosPage extends VBox{
    public ResultadosPage() {
        Label title = new Label("Resultados");

        getChildren().add(title);
    }
}
