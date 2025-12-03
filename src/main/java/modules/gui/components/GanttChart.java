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

public class GanttChart extends Pane {
    
    private final Canvas canvas;
    private final int cellWidth = 30;
    private final int rowHeight = 40;
    private final int headerHeight = 30;
    private final int labelWidth = 80;
    
    private final List<GanttEntry> entries = new ArrayList<>();
    private final Map<String, Color> processColors = new HashMap<>();
    private final List<String> processOrden = new ArrayList<>();//para dibujar antes los procesos
    private final Map<String, GanttEntry> openEntries = new HashMap<>(); //para dibujar desde que empieza

    //para I/O
    private final List<GanttEntry> ioEntries = new ArrayList<>();
    private final Map<String, GanttEntry> openIOEntries = new HashMap<>();

    private int maxTime = 50;
    private int currentTime = 0;

    private String lastActiveProcess = null;

    
    private static final Color[] COLORS = {
        Color.web("#4CAF50"), Color.web("#2196F3"), Color.web("#FF9800"),
        Color.web("#9C27B0"), Color.web("#F44336"), Color.web("#00BCD4"),
        Color.web("#CDDC39"), Color.web("#FF5722"), Color.web("#3F51B5")
    };

    private static final Color IO_COLOR = Color.web("#FF6B6B");
    
    public GanttChart() {
        canvas = new Canvas(800, 400);
        getChildren().add(canvas);
        setPadding(new Insets(10));
        
        draw();
    }

    //cuando inicie
    public void addExecutionStart(String pid, int startTime) {
        Platform.runLater(() -> {
            if (!processColors.containsKey(pid)) {
                processOrden.add(pid);
                processColors.put(pid, COLORS[processColors.size() % COLORS.length]);
            }

            // Crear entrada "abierta" (en progreso)
            GanttEntry entry = new GanttEntry(pid, startTime, startTime); // se ira expandiendo
            openEntries.put(pid, entry);
            entries.add(entry);

            //Marcar este proceso como activo
            lastActiveProcess = pid;
            //System.out.println("[GanttChart] Inicio de ejecución: " + pid + " en t=" + startTime);
            draw();
        });
    }
    
    // cuando un proceso termina su ejecucion
    public void addExecutionEnd(String pid, int endTime) {
        Platform.runLater(()-> {

            GanttEntry entry = openEntries.get(pid);
            if (entry != null) {
                entry.endTime = endTime;
                openEntries.remove(pid);
                //System.out.println("[GanttChart] Fin de ejecucion: " + pid + " en t=" + endTime + " (duración: " + (endTime - entry.startTime) + "u)");
            } else {
                //System.out.println("[GanttChart] No se encontro entrada abierta para " + pid);
            }

            // Limpiar el proceso activo cuando termina
            if (pid.equals(lastActiveProcess)) {
                lastActiveProcess = null;
            }

            if (endTime > maxTime) {
                maxTime = endTime + 10;
            }

            draw();
        });
    }
    
    public void addIOStart(String pid, int startTime) {
        Platform.runLater(() -> {
            // Asegurar que el proceso tenga color asignado
            if (!processColors.containsKey(pid)) {
                processOrden.add(pid);
                processColors.put(pid, COLORS[processColors.size() % COLORS.length]);
            }

            GanttEntry entry = new GanttEntry(pid, startTime, startTime);
            openIOEntries.put(pid, entry);
            ioEntries.add(entry);

            //System.out.println("[GanttChart]Inicio I/O: " + pid + " en t=" + startTime);
            draw();
        });
    }
    
    public void addIOEnd(String pid, int endTime) {
        Platform.runLater(() -> {
            GanttEntry entry = openIOEntries.get(pid);
            if (entry != null) {
                entry.endTime = endTime;
                openIOEntries.remove(pid);
                
                //System.out.println("[GanttChart] Fin I/O: " + pid + " en t=" + endTime + " (duración: " + (endTime - entry.startTime) + "u)");
            }

            if (endTime > maxTime) {
                maxTime = endTime + 10;
            }

            draw();
        });
    }

    public void setCurrentTime(int time) {
        //System.out.println("[GanttChart] setCurrentTime: " + time);
         Platform.runLater(() -> {
            this.currentTime = time;
            
            // se actualiza el endTime de todos los bloques abiertos al tiempo actual
            for (GanttEntry entry : openEntries.values()) {
                entry.endTime = time;
            }

            //Bloque I/O
            for (GanttEntry entry : openIOEntries.values()) {
                entry.endTime = time;
            }
            
            draw();
        });
    }
    
    public void clear() {
        Platform.runLater(() -> {
            entries.clear();
            processColors.clear();

            ioEntries.clear();
            openIOEntries.clear();

            currentTime = 0;
            maxTime = 50;
            draw();
        });
    }
    
    public void initializeProcesses(List<String> processIds) {
        Platform.runLater(() -> {
            processOrden.clear();
            processColors.clear();
            
            for (int i = 0; i < processIds.size(); i++) {
                String pid = processIds.get(i);
                processOrden.add(pid);
                processColors.put(pid, COLORS[i % COLORS.length]);
                //System.out.println("[GanttChart] Proceso " + pid + " pre-creado con color: " + processColors.get(pid));
            }
            
            draw();
        });
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Ajustar tamaño del canvas
        int totalRows = processOrden.size() + 1;
        double width = labelWidth + (maxTime * cellWidth) + 50;
        double height = headerHeight + (totalRows * rowHeight) + 50;
        canvas.setWidth(width);
        canvas.setHeight(height);
        
        // Fondo
        gc.setFill(Color.web("#171025"));
        gc.fillRect(0, 0, width, height);
        
        // Dibujar header con línea de tiempo
        drawTimelineHeader(gc);

        // Dibujar filas
        for (int i = 0; i < processOrden.size(); i++) {
            String pid = processOrden.get(i);
            drawProcessRow(gc, pid, i);
        }


        
        //Dibujar fila de E/S
        drawIORow(gc, processOrden.size());
        // Dibujar cursor de tiempo actual
        drawCurrentTimeCursor(gc);
    }
    
    private void drawTimelineHeader(GraphicsContext gc) {
        gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#888"));
        
        for (int t = 0; t <= maxTime; t += 5) {
            double x = labelWidth + (t * cellWidth);
            gc.fillText(String.valueOf(t), x, 20);//numero del tiempo
            
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
        gc.setFill(Color.web("#171025"));
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
                boolean isOpen = openEntries.containsKey(pid);
                drawExecutionBlock(gc, entry, y,isOpen);
            }
        }
    }

    //Dibujar fila de E/S
    private void drawIORow(GraphicsContext gc, int row) {
        double y = headerHeight + (row * rowHeight);
        
        // Etiqueta "E/S"
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.setFill(IO_COLOR);
        gc.fillText("E/S", 10, y + 25);
        
        // Fondo
        gc.setFill(Color.web("#171025"));
        gc.fillRect(labelWidth, y, maxTime * cellWidth, rowHeight - 5);
        
        // Grid
        gc.setStroke(Color.web("#555"));
        for (int t = 0; t <= maxTime; t++) {
            double x = labelWidth + (t * cellWidth);
            gc.strokeLine(x, y, x, y + rowHeight - 5);
        }
        
        // Dibujar bloques de E/S
        for (GanttEntry entry : ioEntries) {
            boolean isOpen = openIOEntries.containsKey(entry.pid);
            // Usar el color del proceso, no el color de E/S genérico
            Color processColor = processColors.get(entry.pid);
            drawIOBlock(gc, entry, y, isOpen, processColor != null ? processColor : IO_COLOR);
        }
    }

    
    private void drawExecutionBlock(GraphicsContext gc, GanttEntry entry, double y, boolean isOpen) {
        double x = labelWidth + (entry.startTime * cellWidth);
        //+1 para dibujarlo bien
        double width = (entry.endTime - entry.startTime+1) * cellWidth;
        
        //por si esta abierto pero no ha avanzado, no dibujar nada aún
        if (width < 1) {
            return;
        }

        Color color = processColors.get(entry.pid);
        
        // Bloque principal
        gc.setFill(color);
        gc.fillRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        if (isOpen) {
            
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(3);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
            
            // Mostrar "..." en lugar de duracion
            if (width > 20) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
                gc.fillText("...", x + width / 2 - 8, y + 25);
            }

        }else{
            // Borde normal para bloques completados
            gc.setStroke(Color.web("rgba(255,255,255,0.3)"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
            // Texto con duración
            if (width > 20) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
                
                //+1 para dibujarlo bien
                String text = (entry.endTime - entry.startTime +1) + "u";
                gc.fillText(text, x + width / 2 - 10, y + 25);
            }

        }
    }

    //Dibujar bloque de E/S con nombre del proceso
    private void drawIOBlock(GraphicsContext gc, GanttEntry entry, double y, boolean isOpen, Color color) {
        double x = labelWidth + (entry.startTime * cellWidth);
        //falat verificar si se suma +1
        double width = (entry.endTime - entry.startTime +1) * cellWidth;
        
        if (width < 1) {
            return;
        }
        
        // Bloque principal con el color del proceso
        gc.setFill(color);
        gc.fillRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
        
        if (isOpen) {
            // Borde animado
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(3);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
            
            // Mostrar PID del proceso en I/O
            if (width > 15) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                gc.fillText(entry.pid, x + 5, y + 22);
            }
        } else {
            // Borde normal
            gc.setStroke(Color.web("rgba(255,255,255,0.3)"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 2, y + 5, width - 4, rowHeight - 15, 4, 4);
            
            // Mostrar PID y duración
            if (width > 30) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                //falat verificar si se suma +1
                String text = entry.pid + " " + (entry.endTime - entry.startTime+1) + "u";
                gc.fillText(text, x + 5, y + 22);
            } else if (width > 15) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                gc.fillText(entry.pid, x + 5, y + 22);
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
    //Metodos para autoscroll al avanzar el cursor
     public int getActiveProcessRow() {
        if (lastActiveProcess == null) {
            return -1; // No hay proceso activo
        }
        return processOrden.indexOf(lastActiveProcess);
    }
     // Obtener la posición Y del proceso activo
    public double getActiveProcessY() {
        int row = getActiveProcessRow();
        if (row < 0) {
            return 0;
        }
        return headerHeight + (row * rowHeight) + (rowHeight / 2.0); // Centro de la fila
    }
    public double getRowHeight() {
        return rowHeight;
    }

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