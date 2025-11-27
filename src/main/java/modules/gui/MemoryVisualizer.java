package gui;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modules.memory.MemoryManager;
import modules.gui.MemoryGrid;

public class MemoryVisualizer extends Application {
    @Override
    public void start(Stage stage) {
        VBox root = new VBox();

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Visualizador de Memoria");
        stage.setScene(scene);
        stage.show();
        MemoryGrid grid = new MemoryGrid(10);
        root.getChildren().add(grid);
        
        // Simular actualización pequeña
        List<String> estado = List.of("P1 Pg 5", "P2 Pg 1", "P3 Pg 3", null, null, null, "P1 Pg 7", null, "P3 Pg 2", "P2 Pg 2");
        grid.updateFrames(estado, 6, 9); // P1 Pg 7 entra, P2 Pg 2 sale
    }

    public static void main(String[] args) {
        launch();
    }
}