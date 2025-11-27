package modules.gui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.List;

public class MemoryGrid extends GridPane {

    private final int frameCount;
    private final Rectangle[] frameRects;
    private final Label[] frameLabels;

    public MemoryGrid(int frameCount) {
        this.frameCount = frameCount;
        this.frameRects = new Rectangle[frameCount];
        this.frameLabels = new Label[frameCount];

        setHgap(10);
        setVgap(10);
        setAlignment(Pos.CENTER);

        for (int i = 0; i < frameCount; i++) {
            Rectangle rect = new Rectangle(80, 40, Color.LIGHTGRAY);
            Label label = new Label("Libre");
            label.setStyle("-fx-font-weight: bold");

            frameRects[i] = rect;
            frameLabels[i] = label;

            GridPane cell = new GridPane();
            cell.setAlignment(Pos.CENTER);
            cell.add(rect, 0, 0);
            cell.add(label, 0, 1);

            add(cell, i % 5, i / 5); // 5 columnas por fila
        }
    }

    public void updateFrames(List<String> frameContents, int pageInIndex, int pageOutIndex) {
        for (int i = 0; i < frameCount; i++) {
            String content = frameContents.get(i);
            frameLabels[i].setText(content != null ? content : "Libre");
            frameRects[i].setFill(Color.LIGHTGRAY);
        }

        if (pageOutIndex >= 0 && pageOutIndex < frameCount) {
            animatePageOut(pageOutIndex);
        }

        if (pageInIndex >= 0 && pageInIndex < frameCount) {
            animatePageIn(pageInIndex);
        }
    }

    private void animatePageOut(int index) {
        frameRects[index].setFill(Color.RED);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), frameRects[index]);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void animatePageIn(int index) {
        frameRects[index].setFill(Color.GREEN);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), frameRects[index]);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.play();
    }
}