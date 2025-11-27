package gui.pages;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardPage extends VBox {

    public DashboardPage() {
        Label title = new Label("Dashboard");

        getChildren().add(title);
    }
}
