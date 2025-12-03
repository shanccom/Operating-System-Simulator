package modules.gui.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GanttChartSimple extends Pane {
    
    private final Canvas canvas;
    private final int cellWidth = 30;
    private final int rowHeight = 50;
    private final int headerHeight = 30;
    private final int labelWidth = 80;
    
    private final List<GanttEntry> cpuEntries = new ArrayList<>();
    private final List<GanttEntry> ioEntries = new ArrayList<>();
    private final Map<String, Color> processColors = new HashMap<>();
    private final Map<String, GanttEntry> openCpuEntries = new HashMap<>();
    private final Map<String, GanttEntry> openIOEntries = new HashMap<>();
    private final List<GanttEntry> contextSwitchEntries = new ArrayList<>();


    private int maxTime = 50;
    private int currentTime = 0;
    
    private static final Color[] COLORS = {
        Color.web("#4CAF50"), Color.web("#2196F3"), Color.web("#FF9800"),
        Color.web("#9C27B0"), Color.web("#F44336"), Color.web("#00BCD4"),
        Color.web("#CDDC39"), Color.web("#FF5722"), Color.web("#3F51B5")
    };
    private static final Color CAMBIO_CONTEXTO_COLOR = Color.web("#727272ff"); 

    
    public GanttChartSimple() {
        canvas = new Canvas(800, 200);
        getChildren().add(canvas);
        setPadding(new Insets(10));
        
        draw();
    }

    public void addExecutionStart(String pid, int startTime) {
        Platform.runLater(() -> {
            if (!processColors.containsKey(pid)) {
                processColors.put(pid, COLORS[processColors.size() % COLORS.length]);
            }

            GanttEntry entry = new GanttEntry(pid, startTime, startTime);
            openCpuEntries.put(pid, entry);
            cpuEntries.add(entry);

            draw();
        });
    }
    
    public void addExecutionEnd(String pid, int endTime) {
        Platform.runLater(()-> {
            GanttEntry entry = openCpuEntries.get(pid);
            if (entry != null) {
                entry.endTime = endTime;
                openCpuEntries.remove(pid);
            }

            if (endTime > maxTime) {
                maxTime = endTime + 10;
            }

            draw();
        });
    }
    
    public void addIOStart(String pid, int startTime) {
        Platform.runLater(() -> {
            if (!processColors.containsKey(pid)) {
                processColors.put(pid, COLORS[processColors.size() % COLORS.length]);
            }

            GanttEntry entry = new GanttEntry(pid, startTime, startTime);
            openIOEntries.put(pid, entry);
            ioEntries.add(entry);

            draw();
        });
    }
    
    public void addIOEnd(String pid, int endTime) {
        Platform.runLater(() -> {
            GanttEntry entry = openIOEntries.get(pid);
            if (entry != null) {
                entry.endTime = endTime;
                openIOEntries.remove(pid);
            }

            if (endTime > maxTime) {
                maxTime = endTime + 10;
            }

            draw();
        });
    }
    // metodo para agregar context switch
    public void addContextSwitchBlock(String pid, int startTime, int duration) {
        Platform.runLater(() -> {
            int endTime = startTime + duration;
            GanttEntry entry = new GanttEntry(pid, startTime, endTime);
            contextSwitchEntries.add(entry);
            
            if (endTime > maxTime) {
                maxTime = endTime + 10;
            }
            
            System.out.println("[GanttChartSimple] Context Switch agregado: " + pid + 
                            " desde t=" + startTime + " hasta t=" + endTime);
            draw();
        });
    }


    public void setCurrentTime(int time) {
         Platform.runLater(() -> {
            this.currentTime = time;
            
            for (GanttEntry entry : openCpuEntries.values()) {
                entry.endTime = time;
            }

            for (GanttEntry entry : openIOEntries.values()) {
                entry.endTime = time;
            }
            
            draw();
        });
    }
    
    public void clear() {
        Platform.runLater(() -> {
            cpuEntries.clear();
            ioEntries.clear();
            processColors.clear();
            openCpuEntries.clear();
            openIOEntries.clear();
            contextSwitchEntries.clear();
            currentTime = 0;
            maxTime = 50;
            draw();
        });
    }
    
    public void initializeProcesses(List<String> processIds) {
        Platform.runLater(() -> {
            processColors.clear();
            
            for (int i = 0; i < processIds.size(); i++) {
                String pid = processIds.get(i);
                processColors.put(pid, COLORS[i % COLORS.length]);
            }
            
            draw();
        });
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Ajustar tamaño del canvas (solo 2 filas: CPU y I/O)
        double width = labelWidth + (maxTime * cellWidth) + 50;
        double height = headerHeight + (2 * rowHeight) + 50;
        canvas.setWidth(width);
        canvas.setHeight(height);
        
        // Fondo
        gc.setFill(Color.web("#171025"));
        gc.fillRect(0, 0, width, height);
        
        // Dibujar header con línea de tiempo
        drawTimelineHeader(gc);

        // Dibujar fila de CPU
        drawCPURow(gc, 0);
        
        // Dibujar fila de I/O
        drawIORow(gc, 1);
        
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
    
    private void drawCPURow(GraphicsContext gc, int row) {
        double y = headerHeight + (row * rowHeight);
        
        // Etiqueta "CPU"
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        gc.setFill(Color.web("#4CAF50"));
        gc.fillText("CPU", 10, y + 30);
        
        // Fondo de la fila
        gc.setFill(Color.web("#171025"));
        gc.fillRect(labelWidth, y, maxTime * cellWidth, rowHeight - 5);
        
        // Grid horizontal
        gc.setStroke(Color.web("#555"));
        gc.strokeLine(labelWidth, y + rowHeight - 5, labelWidth + (maxTime * cellWidth), y + rowHeight - 5);
        
        // Dibujar bloques de ejecución CPU
        for (GanttEntry entry : cpuEntries) {
            boolean isOpen = openCpuEntries.containsKey(entry.pid);
            drawBlock(gc, entry, y, isOpen, false);
        }

        // Dibujar bloques de cambio de contexto
        for (GanttEntry entry : contextSwitchEntries) {
            drawContextSwitchBlock(gc, entry, y);
        }
        
    }

    private void drawIORow(GraphicsContext gc, int row) {
        double y = headerHeight + (row * rowHeight);
        
        // Etiqueta "I/O"
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        gc.setFill(Color.web("#FF6B6B"));
        gc.fillText("I/O", 10, y + 30);
        
        // Fondo
        gc.setFill(Color.web("#171025"));
        gc.fillRect(labelWidth, y, maxTime * cellWidth, rowHeight - 5);
        
        // Grid horizontal
        gc.setStroke(Color.web("#555"));
        gc.strokeLine(labelWidth, y + rowHeight - 5, labelWidth + (maxTime * cellWidth), y + rowHeight - 5);
        
        // Dibujar bloques de I/O
        for (GanttEntry entry : ioEntries) {
            boolean isOpen = openIOEntries.containsKey(entry.pid);
            drawBlock(gc, entry, y, isOpen,false );
        }
    }
    
    private void drawBlock(GraphicsContext gc, GanttEntry entry, double y, boolean isOpen, boolean isContextSwitch) {
        double x = labelWidth + (entry.startTime * cellWidth);
        double width = (entry.endTime - entry.startTime + 1) * cellWidth;
        
        if (width < 1) {
            return;
        }

        Color color = isContextSwitch ? CAMBIO_CONTEXTO_COLOR: processColors.get(entry.pid);
        
        // Bloque principal
        gc.setFill(color);
        gc.fillRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        if (isOpen) {
            // Borde animado para bloques en progreso
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(3);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
            
            // Mostrar PID y "..." mientras está en progreso
            if (width > 25) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
                gc.fillText(entry.pid + " ...", x + 5, y + 28);
            } else if (width > 15) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                gc.fillText(entry.pid, x + 5, y + 28);
            }
        } else {
            // Borde normal para bloques completados
            gc.setStroke(Color.web("rgba(255,255,255,0.3)"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
            
            // Mostrar PID y duración
            int duration = entry.endTime - entry.startTime + 1;
            String text = entry.pid + " (" + duration + "u)";
            
            if (width > 60) {
                // Texto completo
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
                gc.fillText(text, x + 5, y + 28);
            } else if (width > 40) {
                // Solo PID y duración más corta
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                gc.fillText(entry.pid + " " + duration + "u", x + 5, y + 28);
            } else if (width > 25) {
                // Solo PID
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                gc.fillText(entry.pid, x + 5, y + 28);
            } else if (width > 15) {
                // Solo duración
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
                gc.fillText(duration + "u", x + 3, y + 28);
            }
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

    private void drawContextSwitchBlock(GraphicsContext gc, GanttEntry entry, double y) {
        double x = labelWidth + (entry.startTime * cellWidth);
        double width = (entry.endTime - entry.startTime) * cellWidth;
        
        if (width < 1) {
            return;
        }
        
        // Bloque principal
        gc.setFill(CAMBIO_CONTEXTO_COLOR);
        gc.fillRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        // Patron de rayas diagonales
        gc.setStroke(Color.web("#FFFFFF"));
        gc.setLineWidth(1);
        for (int i = 0; i < width; i += 4) {
            gc.strokeLine(x + 2 + i, y + 5, x + 2 + i + 5, y + rowHeight - 10);
        }
        
        // Borde
        gc.setStroke(Color.web("#c5c5c5ff")); 
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        // Texto "CS" + PID
        if (width > 30) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
            gc.fillText("CS:" + entry.pid, x + 5, y + 28);
        } else if (width > 15) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
            gc.fillText("CS", x + 5, y + 28);
        }
    }

    
    // Métodos para autoscroll
    public double getCurrentCursorX() {
        return labelWidth + (currentTime * cellWidth);
    }
    
    public double getTotalWidth() {
        return canvas.getWidth();
    }
    
    public double getTotalHeight() {
        return canvas.getHeight();
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