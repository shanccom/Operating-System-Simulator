package modules.gui.components;

import javafx.animation.FillTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import modules.memory.MemoryEventListener;

public class MemoryVisualizer extends VBox implements MemoryEventListener {

    private HBox framesRow;
    private Rectangle[] frameRects;
    private Text[] frameLabels;

    public MemoryVisualizer(int totalFrames) {
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        initialize(totalFrames);
    }

    public void initialize(int totalFrames) {
        // Limpia si ya existía algo
        getChildren().clear();

        framesRow = new HBox(12);
        framesRow.setAlignment(Pos.CENTER);

        frameRects = new Rectangle[totalFrames];
        frameLabels = new Text[totalFrames];

        for (int i = 0; i < totalFrames; i++) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(10));

            Rectangle r = new Rectangle(70, 70);
            r.setArcWidth(14);
            r.setArcHeight(14);
            r.setFill(Color.web("#1a102b"));

            Text label = new Text("FREE");
            label.getStyleClass().add("text-clear");

            frameRects[i] = r;
            frameLabels[i] = label;

            card.getChildren().addAll(r, label);
            framesRow.getChildren().add(card);
        }

        getChildren().add(framesRow);
    }

    @Override
    public void onPageAccess(int frameIndex, String pid, int page, boolean hit) {
        Platform.runLater(() -> {
            if (frameIndex >= 0 && frameIndex < frameLabels.length) {
                frameLabels[frameIndex].setText(pid + " | P" + page);
                if (hit) animateColor(frameIndex, Color.web("#3cff8a")); // verde suave
            }
        });
    }

    @Override
    public void onPageFault(String pid, int page) {
        Platform.runLater(() -> {
            for (Rectangle r : frameRects) {
                animateFlash(r, Color.web("#ff4f6d")); // rojo por page fault
            }
        });
    }

    @Override
    public void onFrameLoaded(int frameIndex, String pid, int page) {
        Platform.runLater(() -> {
            if (frameIndex >= 0 && frameIndex < frameLabels.length) {
                frameLabels[frameIndex].setText(pid + " | P" + page);
                animateColor(frameIndex, Color.web("#52ffa8"));
            }
        });
    }

    @Override
    public void onFrameEvicted(int frameIndex, String oldPid, int oldPage) {
        Platform.runLater(() -> {
            if (frameIndex >= 0 && frameIndex < frameLabels.length) {
                frameLabels[frameIndex].setText("FREE");
                animateColor(frameIndex, Color.web("#ffb14d"));
            }
        });
    }

    @Override
    public void onVictimChosen(int frameIndex, String reason) {
        Platform.runLater(() -> {
            if (frameIndex >= 0 && frameIndex < frameLabels.length) {
                animateColor(frameIndex, Color.web("#ffe066"));
            }
        });
    }

    @Override
    public void onSnapshot(String snapshot) {
        // opcional: podrías mostrar un panel lateral
    }

    // =============================
    //      ANIMACIONES
    // =============================

    private void animateColor(int frameIndex, Color color) {
        Rectangle r = frameRects[frameIndex];
        FillTransition ft = new FillTransition(Duration.millis(300), r);
        ft.setFromValue(Color.web("#1a102b"));
        ft.setToValue(color);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.play();
    }

    private void animateFlash(Rectangle r, Color color) {
        FillTransition ft = new FillTransition(Duration.millis(180), r);
        ft.setToValue(color);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.play();
    }
}
