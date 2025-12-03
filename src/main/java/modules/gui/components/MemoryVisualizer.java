package modules.gui.components;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Config.ReplacementType;
import modules.memory.MemoryEventListener;
/* Aca se visualiza la memoria fisica y las tablas de paginas de los procesos mediante disparadores se realizan efectos visuales 
con tiempo medido y explicacion en logger para una correcta comprension
Para esta parte en especifico se implemento pasos de espera entre solo este modulo a cambio del pasos por tiempo del resto del sistema 
y se implemento el patron singleton en la clase MemoryVisualizer */
public class MemoryVisualizer extends VBox implements MemoryEventListener {

    private int totalFrames;
    private VBox physicalFramesContainer;
    private VBox pageTablesContainer;
    private Label algorithmLabel;
    private Label victimInfoLabel;
    private static double ANIMATION_SPEED = 8; // mas lento


    // Mapa de procesos -> sus tablas de páginas visuales
    private Map<String, ProcessPageTable> processPageTables = new LinkedHashMap<>();
    private Map<Integer, PhysicalFrameCard> physicalFrames = new HashMap<>();
    
    private String currentAlgorithm;
    private Color[] processColors = {
        Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
        Color.web("#FFA07A"), Color.web("#5086f2ff"), Color.web("#F7DC6F"),
        Color.web("#BB8FCE"), Color.web("#a669ecff")
    };
    private int colorIndex = 0;

    // Constructor sin parámetros para mostrar UI vacía
    public MemoryVisualizer() {
        setSpacing(15);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_CENTER);
        buildEmptyUI();
    }

    public MemoryVisualizer(ReplacementType algor, int frames) {
        this();
        initialize(algor, frames);
    }

    // Construye la UI sin frames físicos ni configuración
    private void buildEmptyUI() {
        getChildren().clear();
        processPageTables.clear();
        physicalFrames.clear();
        colorIndex = 0;
        
        getStyleClass().add("card-mem");
        
        Label title = new Label("Memoria");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffffff;");
        
        // Contenedor principal horizontal
        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(mainContent, Priority.ALWAYS);
        mainContent.setMaxWidth(Double.MAX_VALUE);
        
        // Panel izquierdo: Tablas de Páginas de procesos
        VBox leftPanel = new VBox(10);
        leftPanel.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        leftPanel.setMaxWidth(Double.MAX_VALUE);
        
        Label pageTableTitle = new Label("Tabla de Páginas");
        pageTableTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffffff;");
        
        ScrollPane pageTableScroll = new ScrollPane();
        pageTablesContainer = new VBox(10);
        pageTablesContainer.setPadding(new Insets(10));
        pageTableScroll.setContent(pageTablesContainer);
        pageTableScroll.setFitToWidth(true);
        VBox.setVgrow(pageTableScroll, Priority.ALWAYS);
        HBox.setHgrow(pageTablesContainer, Priority.ALWAYS);
        pageTablesContainer.setMaxWidth(Double.MAX_VALUE);
        
        pageTableScroll.setMaxHeight(350);
        pageTableScroll.setStyle("-fx-background: #1a102b; -fx-background-color: transparent;");
        
        leftPanel.getChildren().addAll(pageTableTitle, pageTableScroll);
        
        // Marcos Físicos
        VBox rightPanel = new VBox(10);
        rightPanel.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        rightPanel.setMaxWidth(Double.MAX_VALUE);
        
        Label physicalTitle = new Label("Marcos Físicos");
        physicalTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffffff;");
        
        ScrollPane physicalScroll = new ScrollPane();
        physicalFramesContainer = new VBox(8);
        physicalFramesContainer.setPadding(new Insets(10));
        physicalScroll.setContent(physicalFramesContainer);
        physicalScroll.setFitToWidth(true);
        physicalScroll.setMaxHeight(350);
        physicalScroll.setStyle("-fx-background: #1a102b; -fx-background-color: transparent;");
        VBox.setVgrow(physicalScroll, Priority.ALWAYS);
        HBox.setHgrow(physicalFramesContainer, Priority.ALWAYS);
        physicalFramesContainer.setMaxWidth(Double.MAX_VALUE);

        rightPanel.getChildren().addAll(physicalTitle, physicalScroll);
        
        mainContent.getChildren().addAll(leftPanel, rightPanel);
        
        // Panel inferior: Información del algoritmo y víctima
        VBox bottomPanel = new VBox(5);
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        bottomPanel.setPadding(new Insets(5));
        bottomPanel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8px;");
        
        algorithmLabel = new Label("Algoritmo: No configurado");
        algorithmLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff36bcff; -fx-font-weight: bold;");
        
        victimInfoLabel = new Label("Esperando configuración...");
        victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaaaa;");
        
        bottomPanel.getChildren().addAll(algorithmLabel, victimInfoLabel);
        
        getChildren().addAll(title, mainContent, bottomPanel);
    }
    
    public void initialize(ReplacementType algor, int frames) {
        this.totalFrames = frames;
        this.currentAlgorithm = algor.name();

        
        // Limpiar datos anteriores
        //pageTablesContainer.clear(); //problemas
        if (pageTablesContainer != null) {
            pageTablesContainer.getChildren().clear(); 
        }
        processPageTables.clear();
        physicalFrames.clear();
        colorIndex = 0;
        
        // Actualizar UI existente
        if (algorithmLabel != null) {
            algorithmLabel.setText("Algoritmo: " + currentAlgorithm);
        }
        
        if (victimInfoLabel != null) {
            victimInfoLabel.setText("Esperando eventos...");
        }
        
        // Construir los frames físicos
        buildPhysicalFrames();
        //System.out.println("[MemoryVisualizer] Frames creados: " + physicalFrames.size() + ", Keys: " + physicalFrames.keySet());
    }

    // Construye los frames físicos cuando ya hay configuración
    public void buildPhysicalFrames() {
        if (physicalFramesContainer == null) {
            System.err.println("[MemoryVisualizer ERROR] physicalFramesContainer es null!");
            return;
        }
        
        physicalFramesContainer.getChildren().clear();
        physicalFrames.clear();

        //System.out.println("[MemoryVisualizer] buildPhysicalFrames: construyendo " + totalFrames + " frames...");
        for (int i = 0; i < totalFrames; i++) {
            PhysicalFrameCard frameCard = new PhysicalFrameCard(i);
            physicalFrames.put(i, frameCard);
            physicalFramesContainer.getChildren().add(frameCard);
            System.out.println("  - Frame " + i + " creado");
        }
    }

    public void setAlgorithm(String algorithm) {
        this.currentAlgorithm = algorithm;
        Platform.runLater(() -> {
            if (algorithmLabel != null) {
                algorithmLabel.setText("Algoritmo: " + algorithm);
            }
        });
    }
    
    // Método público que usa Platform.runLater
    public void registerProcess(String pid, int minPages) {
        Platform.runLater(() -> registerProcessSync(pid, minPages));
    }
    
    // Método interno síncrono (debe llamarse desde el JavaFX thread)
    private void registerProcessSync(String pid, int minPages) {
        if (!processPageTables.containsKey(pid)) {
            Color processColor = processColors[colorIndex % processColors.length];
            colorIndex++;
            ProcessPageTable pageTable = new ProcessPageTable(pid, minPages, processColor);
            processPageTables.put(pid, pageTable);
            pageTablesContainer.getChildren().add(pageTable);
        } else {
            // Expandir si es necesario
            processPageTables.get(pid).ensurePageCapacity(minPages);
        }
    }
    
    @Override
    public void onPageAccess(int frameIndex, String pid, int page, boolean hit) {
        Platform.runLater(() -> {
            registerProcessSync(pid, page + 1);  // Registro síncrono
            
            if (hit) {
                if (physicalFrames.containsKey(frameIndex)) {
                    physicalFrames.get(frameIndex).highlightHit();
                } else {
                    System.err.println("[MemoryVisualizer ERROR] Frame " + frameIndex + " no existe en physicalFrames. Total frames: " + physicalFrames.size());
                }
                victimInfoLabel.setText("✓ HIT: Página P" + page + " de " + pid + " cargada en memoria fisica" + frameIndex);
                victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #00ff88;");
            }
        });
    }

    @Override
    public void onPageFault(String pid, int page) {
        Platform.runLater(() -> {
            registerProcessSync(pid, page + 1);  // Registro síncrono
            
            if (processPageTables.containsKey(pid)) {
                processPageTables.get(pid).markPageFault(page);
            }
            
            victimInfoLabel.setText("✗ PAGE FAULT: Página P" + page + " de " + pid + " no encontrada en memoria");
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff4f6d; -fx-font-weight: bold;");
            
            for (PhysicalFrameCard frame : physicalFrames.values()) {
                frame.flashFault();
            }
        });
    }

    @Override
    public void onFrameLoaded(int frameIndex, String pid, int page, long lastAccessTime) {
        System.out.println("[MemoryVisualizer] onFrameLoaded: frame=" + frameIndex + ", pid=" + pid + ", page=" + page);
        Platform.runLater(() -> {
            registerProcessSync(pid, page + 1);  // Registro síncrono PRIMERO
            
            if (processPageTables.containsKey(pid)) {
                processPageTables.get(pid).loadPage(page, frameIndex);
            } else {
                System.err.println("[MemoryVisualizer ERROR] Process " + pid + " no encontrado en processPageTables");
            }
            
            if (physicalFrames.containsKey(frameIndex)) {
                Color processColor = processPageTables.get(pid).getColor();
                physicalFrames.get(frameIndex).load(pid, page, processColor, lastAccessTime);
            } else {
                //System.err.println("[MemoryVisualizer ERROR] Frame " + frameIndex + " no existe en physicalFrames. Total frames: " + physicalFrames.size());
            }
            
            victimInfoLabel.setText("↑ CARGADO: P" + page + " de " + pid + " → Frame " + frameIndex);
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #52ffa8;");
        });
    }

    @Override
    public void onFrameEvicted(int frameIndex, String oldPid, int oldPage) {
        System.out.println("[MemoryVisualizer] onFrameEvicted: frame=" + frameIndex + ", oldPid=" + oldPid + ", oldPage=" + oldPage);
        Platform.runLater(() -> {
            if (processPageTables.containsKey(oldPid)) {
                processPageTables.get(oldPid).evictPage(oldPage);
            }
            
            if (physicalFrames.containsKey(frameIndex)) {
                physicalFrames.get(frameIndex).evict();
            } else {
                System.err.println("[MemoryVisualizer ERROR] Frame " + frameIndex + " no existe en physicalFrames al hacer evict");
            }
            
            victimInfoLabel.setText("↓ EVICTED: P" + oldPage + " de " + oldPid + " removido del Frame " + frameIndex);
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffb14d;");
        });
    }

    @Override
    public void onVictimChosen(int frameIndex, String reason, long lastAccessTime) {
        //System.out.println("[MemoryVisualizer] onVictimChosen: frame=" + frameIndex + ", reason=" + reason);
        Platform.runLater(() -> {
            if (physicalFrames.containsKey(frameIndex)) {
                physicalFrames.get(frameIndex).highlightVictim(lastAccessTime);
            } else {
                System.err.println("[MemoryVisualizer ERROR] Frame " + frameIndex + " no existe en physicalFrames al elegir víctima");
            }
            
            String victimInfo = "Frame " + frameIndex + " candidato (" + currentAlgorithm + ")";
            if (reason != null && !reason.isEmpty()) {
                victimInfo += " - " + reason;
            }
            victimInfo += " [LastAccess=" + lastAccessTime + "]";
            victimInfoLabel.setText("⚠ VICTIMA: " + victimInfo);
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffe066; -fx-font-weight: bold;");
        });
    }

    @Override
    public void onSnapshot(String snapshot) {
        
    }
    @Override
    public void onPageAccessed(int frameIndex, String pid, int page, long newAccessTime) {
        Platform.runLater(() -> {
            PhysicalFrameCard frameBox = physicalFrames.get(frameIndex);
            if (frameBox != null) {
                frameBox.updateAccessTime(newAccessTime);
            }
        });
    }
    
    
    
    private class ProcessPageTable extends VBox {
        private String pid;
        private Color processColor;
        private Map<Integer, PageEntry> pages = new HashMap<>();
        private GridPane pageGrid;
        
        public ProcessPageTable(String pid, int totalPages, Color processColor) {
            this.pid = pid;
            this.processColor = processColor;
            setSpacing(5);
            setPadding(new Insets(8));
            setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 6px; -fx-border-color: " + toRgbString(processColor) + "; -fx-border-radius: 6px; -fx-border-width: 2px;");
            setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(this, Priority.ALWAYS);
            
            Label header = new Label(pid);
            header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + toRgbString(processColor) + ";");
            
            pageGrid = new GridPane();
            pageGrid.setHgap(5);
            pageGrid.setVgap(5);
            pageGrid.setMaxWidth(Double.MAX_VALUE);
            
            Label pageHeader = new Label("Página");
            pageHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
            Label frameHeader = new Label("Frame");
            frameHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
            
            pageGrid.add(pageHeader, 0, 0);
            pageGrid.add(frameHeader, 1, 0);
            
            for (int i = 0; i < totalPages; i++) {
                PageEntry entry = new PageEntry(i);
                pages.put(i, entry);
                pageGrid.add(entry, 0, i + 1);
                GridPane.setColumnSpan(entry, 2);
            }
            
            getChildren().addAll(header, pageGrid);
        }
        
        public Color getColor() {
            return processColor;
        }
        
        public void loadPage(int page, int frame) {
            ensurePageCapacity(page + 1);
            if (pages.containsKey(page)) {
                pages.get(page).load(frame);
            }
        }
        
        public void evictPage(int page) {
            ensurePageCapacity(page + 1);
            if (pages.containsKey(page)) {
                pages.get(page).evict();
            }
        }
        
        public void markPageFault(int page) {
            ensurePageCapacity(page + 1);
            if (pages.containsKey(page)) {
                pages.get(page).fault();
            }
        }
        
        public void ensurePageCapacity(int minPages) {
            int currentMax = pages.keySet().stream().max(Integer::compare).orElse(-1);
            if (currentMax < minPages - 1) {
                // Necesitamos agregar más páginas
                for (int i = currentMax + 1; i < minPages; i++) {
                    PageEntry entry = new PageEntry(i);
                    pages.put(i, entry);
                    pageGrid.add(entry, 0, i + 1);
                    GridPane.setColumnSpan(entry, 2);
                }
            }
        }
        
        private String toRgbString(Color color) {
            return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
        }
    }
    
    private class PageEntry extends HBox {
        private int pageNumber;
        private Label pageLabel;
        private Label frameLabel;
        private boolean loaded = false;
        
        public PageEntry(int pageNumber) {
            this.pageNumber = pageNumber;
            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(3, 8, 3, 8));
            setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4px;");
            setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(this, Priority.ALWAYS);
            
            pageLabel = new Label("" + pageNumber);
            pageLabel.setMinWidth(40);
            pageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff;");
            
            frameLabel = new Label("---");
            frameLabel.setMinWidth(40);
            frameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
            HBox.setHgrow(frameLabel, Priority.ALWAYS);
            
            getChildren().addAll(pageLabel, frameLabel);
        }
        
        public void load(int frame) {
            loaded = true;
            frameLabel.setText("P" + frame);
            frameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00ff88; -fx-font-weight: bold;");
            
            // Delay inicial de 3 segundos antes de aplicar el estilo
            PauseTransition initialPause = new PauseTransition(Duration.seconds(3));
            initialPause.setOnFinished(e -> {
                // Animación lenta usando ANIMATION_SPEED
                setStyle("-fx-background-color: rgba(0,255,136,0.1); -fx-background-radius: 4px;");
                
                PauseTransition animPause = new PauseTransition(Duration.millis(700 * ANIMATION_SPEED));
                animPause.setOnFinished(ev -> {
                    // Aquí podrías dejar el estilo final o hacer otra transición
                });
                animPause.play();
            });
            initialPause.play();

        }
        
        public void evict() {
            loaded = false;
            frameLabel.setText("---");
            frameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
            setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4px;");
        }
        
        public void fault() {
            setStyle("-fx-background-color: rgba(255,79,109,0.2); -fx-background-radius: 4px;");
            PauseTransition pause = new PauseTransition(Duration.millis(700*ANIMATION_SPEED));
            pause.setOnFinished(e -> {
                if (!loaded) {
                    setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4px;");
                }
            });
            pause.play();
        }
    }
    
    private class PhysicalFrameCard extends HBox {
        private int frameIndex;
        private Label frameLabel;
        private Label timeLabel;  // NUEVO
        private long lastAccessTime = 0;  // NUEVO
        private Label contentLabel;
        private Rectangle colorIndicator;
        private boolean occupied = false;
        
        public PhysicalFrameCard(int frameIndex) {
            this.frameIndex = frameIndex;
            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(8));
            setStyle("-fx-background-color: rgba(26,16,43,0.6); -fx-background-radius: 6px; -fx-border-color: #333; -fx-border-radius: 6px;");
            setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(this, Priority.ALWAYS);
            
            frameLabel = new Label("Frame " + frameIndex);
            frameLabel.setMinWidth(70);
            frameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            
            colorIndicator = new Rectangle(8, 30);
            colorIndicator.setArcWidth(4);
            colorIndicator.setArcHeight(4);
            colorIndicator.setFill(Color.web("#333333"));
            
            // Contenedor vertical para contenido y tiempo
            VBox infoBox = new VBox(2);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            contentLabel = new Label("Vacío");
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-font-style: italic;");
            HBox.setHgrow(contentLabel, Priority.ALWAYS);
            timeLabel = new Label("");  
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            infoBox.getChildren().addAll(contentLabel, timeLabel);
            getChildren().addAll(frameLabel, colorIndicator, contentLabel, infoBox);
        }
        
        public void load(String pid, int page, Color processColor, long lastAccessTime) {
            occupied = true;
            contentLabel.setText("Pid: " + pid + " (Page" + page + ")");
            timeLabel.setText("⏱ Last Access : " + lastAccessTime);
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00ff88;");
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
            colorIndicator.setFill(processColor);
            setStyle("-fx-background-color: rgba(0,255,136,0.1); -fx-background-radius: 6px; -fx-border-color: " + toRgbString(processColor) + "; -fx-border-radius: 6px; -fx-border-width: 2px;");
        }
        
        public void evict() {
            occupied = false;
            contentLabel.setText("FREE");
            timeLabel.setText("");  // se vacia
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-font-style: italic;");
            colorIndicator.setFill(Color.web("#333333"));
            setStyle("-fx-background-color: rgba(26,16,43,0.6); -fx-background-radius: 6px; -fx-border-color: #333; -fx-border-radius: 6px;");
        }

        public void updateAccessTime(long newAccessTime) {
            timeLabel.setText("⏱ Last Access : " + newAccessTime);
        }
        public void highlightHit() {
            String currentStyle = getStyle();
            setStyle(currentStyle + "; -fx-background-color: rgba(0,255,136,0.3);");
            
            // Actualizar el label de tiempo con efecto
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00ff88; -fx-font-weight: bold;");
            
            PauseTransition pause = new PauseTransition(Duration.millis(400*ANIMATION_SPEED));
            pause.setOnFinished(e -> {
                setStyle(currentStyle);
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00ff88;");
            });
            pause.play();
        }
        
        public void highlightVictim(long victimTime) {
            String currentContent = contentLabel.getText();
            String currentStyle = getStyle();
            // Mostrar que es víctima y el tiempo que tenía
            contentLabel.setText("< VICTIM (T=" + victimTime + ")");
            timeLabel.setText("⏱ Oldest: " + victimTime); 
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff6666ff; -fx-font-weight: bold;");
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd427ff; -fx-font-weight: bold;");
            setStyle(currentStyle + "; -fx-border-color: #ffe066; -fx-border-width: 3px;");
            PauseTransition pause = new PauseTransition(Duration.millis(1500*ANIMATION_SPEED));
            pause.setOnFinished(e -> {
                contentLabel.setText(currentContent);
                contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00ff88;");
                setStyle(currentStyle);
            });
            pause.play();
        }
        
        public void flashFault() {
            String currentStyle = getStyle();
            setStyle(currentStyle + "; -fx-background-color: rgba(255,79,109,0.2);");
            PauseTransition pause = new PauseTransition(Duration.millis(400*ANIMATION_SPEED));
            pause.setOnFinished(e -> setStyle(currentStyle));
            pause.play();
        }
        
        private String toRgbString(Color color) {
            return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
        }
    }
}