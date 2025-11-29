package modules.gui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Process;

import java.util.*;

public class GanttChart extends Pane {
    
    private final Canvas canvas;
    private final int cellWidth = 30;
    private final int rowHeight = 40;
    private final int headerHeight = 30;
    private final int labelWidth = 80;
    
    private final List<GanttEntry> entries = new ArrayList<>();
    private final Map<String, Color> processColors = new HashMap<>();
    private int maxTime = 50;
    private int currentTime = 0;
    
    private static final Color[] COLORS = {
        Color.web("#4CAF50"), Color.web("#2196F3"), Color.web("#FF9800"),
        Color.web("#9C27B0"), Color.web("#F44336"), Color.web("#00BCD4"),
        Color.web("#CDDC39"), Color.web("#FF5722"), Color.web("#3F51B5")
    };
    
    public GanttChart() {
        canvas = new Canvas(800, 400);
        getChildren().add(canvas);
        setPadding(new Insets(10));
        
        draw();
    }
    
    public void addExecution(String pid, int startTime, int endTime) {
         System.out.println("[GanttChart] addExecution: " + pid + " [" + startTime + "-" + endTime + "]");
        Platform.runLater(() -> {
            if (!processColors.containsKey(pid)) {
                processColors.put(pid, COLORS[processColors.size() % COLORS.length]);
                System.out.println("[GanttChart] Color asignado a " + pid + ": " + processColors.get(pid));
            }
            
            entries.add(new GanttEntry(pid, startTime, endTime));
            System.out.println("[GanttChart] Total entries: " + entries.size());
            
            if (endTime > maxTime) {
                maxTime = endTime + 10;
            }
            
            draw();
        });
    }
    
    public void setCurrentTime(int time) {
        System.out.println("[GanttChart] setCurrentTime: " + time);
        Platform.runLater(() -> {
            this.currentTime = time;
            draw();
        });
    }
    
    public void clear() {
        Platform.runLater(() -> {
            entries.clear();
            processColors.clear();
            currentTime = 0;
            maxTime = 50;
            draw();
        });
    }
    
    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Ajustar tamaño del canvas
        double width = labelWidth + (maxTime * cellWidth) + 50;
        double height = headerHeight + (processColors.size() * rowHeight) + 50;
        canvas.setWidth(width);
        canvas.setHeight(height);
        
        // Fondo
        gc.setFill(Color.web("#2d2d2d"));
        gc.fillRect(0, 0, width, height);
        
        // Dibujar header con línea de tiempo
        drawTimelineHeader(gc);
        
        // Dibujar filas de procesos
        int row = 0;
        for (String pid : processColors.keySet()) {
            drawProcessRow(gc, pid, row);
            row++;
        }
        
        // Dibujar cursor de tiempo actual
        drawCurrentTimeCursor(gc);
    }
    
    private void drawTimelineHeader(GraphicsContext gc) {
        gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#888"));
        
        for (int t = 0; t <= maxTime; t += 5) {
            double x = labelWidth + (t * cellWidth);
            gc.fillText(String.valueOf(t), x, 20);
            
            // Línea vertical de grid
            gc.setStroke(Color.web("#444"));
            gc.strokeLine(x, headerHeight, x, canvas.getHeight());
        }
    }
    
    private void drawProcessRow(GraphicsContext gc, String pid, int row) {
        double y = headerHeight + (row * rowHeight);
        
        // Etiqueta del proceso
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.setFill(processColors.get(pid));
        gc.fillText(pid, 10, y + 25);
        
        // Fondo de la fila
        gc.setFill(Color.web("#3d3d3d"));
        gc.fillRect(labelWidth, y, maxTime * cellWidth, rowHeight - 5);
        
        // Dibujar grid lines
        gc.setStroke(Color.web("#555"));
        for (int t = 0; t <= maxTime; t++) {
            double x = labelWidth + (t * cellWidth);
            gc.strokeLine(x, y, x, y + rowHeight - 5);
        }
        
        // Dibujar bloques de ejecución
        for (GanttEntry entry : entries) {
            if (entry.pid.equals(pid)) {
                drawExecutionBlock(gc, entry, y);
            }
        }
    }
    
    private void drawExecutionBlock(GraphicsContext gc, GanttEntry entry, double y) {
        double x = labelWidth + (entry.startTime * cellWidth);
        double width = (entry.endTime - entry.startTime) * cellWidth;
        
        Color color = processColors.get(entry.pid);
        
        // Bloque principal
        gc.setFill(color);
        gc.fillRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        // Borde
        gc.setStroke(Color.web("rgba(255,255,255,0.3)"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        // Texto con duración
        if (width > 20) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            String text = (entry.endTime - entry.startTime) + "u";
            gc.fillText(text, x + width / 2 - 10, y + 25);
        }
        
        // Animación si está ejecutando ahora
        if (entry.startTime <= currentTime && entry.endTime > currentTime) {
            gc.setStroke(Color.web("#4CAF50"));
            gc.setLineWidth(3);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        }
    }
    
    private void drawCurrentTimeCursor(GraphicsContext gc) {
        double x = labelWidth + (currentTime * cellWidth);
        
        gc.setStroke(Color.web("#4CAF50"));
        gc.setLineWidth(2);
        gc.strokeLine(x, headerHeight, x, canvas.getHeight());
        
        // Indicador en la parte superior
        gc.setFill(Color.web("#4CAF50"));
        gc.fillOval(x - 4, headerHeight - 8, 8, 8);
    }
    
    private static class GanttEntry {
        String pid;
        int startTime;
        int endTime;
        
        GanttEntry(String pid, int startTime, int endTime) {
            this.pid = pid;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}