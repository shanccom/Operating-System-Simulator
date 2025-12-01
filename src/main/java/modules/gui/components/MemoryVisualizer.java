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

public class MemoryVisualizer extends VBox implements MemoryEventListener {

    private int totalFrames;
    private VBox physicalFramesContainer;
    private VBox pageTablesContainer;
    private Label algorithmLabel;
    private Label victimInfoLabel;

    // Mapa de procesos -> sus tablas de páginas visuales
    private Map<String, ProcessPageTable> processPageTables = new LinkedHashMap<>();
    private Map<Integer, PhysicalFrameCard> physicalFrames = new HashMap<>();
    
    private String currentAlgorithm;
    private Color[] processColors = {
        Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
        Color.web("#FFA07A"), Color.web("#98D8C8"), Color.web("#F7DC6F"),
        Color.web("#BB8FCE"), Color.web("#85C1E2")
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
        
        // Título principal
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
        
        // Panel derecho: Marcos Físicos
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
        algorithmLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff00aaff; -fx-font-weight: bold;");
        
        victimInfoLabel = new Label("Esperando configuración...");
        victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaaaa;");
        
        bottomPanel.getChildren().addAll(algorithmLabel, victimInfoLabel);
        
        getChildren().addAll(title, mainContent, bottomPanel);
    }
    
    public void initialize(ReplacementType algor, int frames) {
        this.totalFrames = frames;
        this.currentAlgorithm = algor.name();
        
        // Limpiar datos anteriores
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
    }

    // Construye los frames físicos cuando ya hay configuración
    public void buildPhysicalFrames() {
        if (physicalFramesContainer == null) return;
        
        physicalFramesContainer.getChildren().clear();
        physicalFrames.clear();

        for (int i = 0; i < totalFrames; i++) {
            PhysicalFrameCard frameCard = new PhysicalFrameCard(i);
            physicalFrames.put(i, frameCard);
            physicalFramesContainer.getChildren().add(frameCard);
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
    
    public void registerProcess(String pid, int minPages) {
        Platform.runLater(() -> {
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
        });
    }
    
    @Override
    public void onPageAccess(int frameIndex, String pid, int page, boolean hit) {
        Platform.runLater(() -> {
            registerProcess(pid, page + 1);  // At least page+1 pages
            
            if (hit) {
                if (physicalFrames.containsKey(frameIndex)) {
                    physicalFrames.get(frameIndex).highlightHit();
                }
                victimInfoLabel.setText("✓ HIT: Página P" + page + " de " + pid + " encontrada en Frame " + frameIndex);
                victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #00ff88;");
            }
        });
    }

    @Override
    public void onPageFault(String pid, int page) {
        Platform.runLater(() -> {
            registerProcess(pid, page + 1);  // At least page+1 pages
            
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
    public void onFrameLoaded(int frameIndex, String pid, int page) {
        Platform.runLater(() -> {
            registerProcess(pid, page + 1);  // At least page+1 pages
            
            if (processPageTables.containsKey(pid)) {
                processPageTables.get(pid).loadPage(page, frameIndex);
            }
            
            if (physicalFrames.containsKey(frameIndex)) {
                Color processColor = processPageTables.get(pid).getColor();
                physicalFrames.get(frameIndex).load(pid, page, processColor);
            }
            
            victimInfoLabel.setText("↑ CARGADO: P" + page + " de " + pid + " → Frame " + frameIndex);
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #52ffa8;");
        });
    }

    @Override
    public void onFrameEvicted(int frameIndex, String oldPid, int oldPage) {
        Platform.runLater(() -> {
            if (processPageTables.containsKey(oldPid)) {
                processPageTables.get(oldPid).evictPage(oldPage);
            }
            
            if (physicalFrames.containsKey(frameIndex)) {
                physicalFrames.get(frameIndex).evict();
            }
            
            victimInfoLabel.setText("↓ EVICTED: P" + oldPage + " de " + oldPid + " removido del Frame " + frameIndex);
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffb14d;");
        });
    }

    @Override
    public void onVictimChosen(int frameIndex, String reason) {
        Platform.runLater(() -> {
            if (physicalFrames.containsKey(frameIndex)) {
                physicalFrames.get(frameIndex).highlightVictim();
            }
            
            String victimInfo = "Frame " + frameIndex + " candidato (" + currentAlgorithm + ")";
            if (reason != null && !reason.isEmpty()) {
                victimInfo += " - " + reason;
            }
            victimInfoLabel.setText("⚠ VICTIMA: " + victimInfo);
            victimInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffe066; -fx-font-weight: bold;");
        });
    }

    @Override
    public void onSnapshot(String snapshot) {
        
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
            setStyle("-fx-background-color: rgba(0,255,136,0.1); -fx-background-radius: 4px;");
        }
        
        public void evict() {
            loaded = false;
            frameLabel.setText("---");
            frameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
            setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4px;");
        }
        
        public void fault() {
            setStyle("-fx-background-color: rgba(255,79,109,0.2); -fx-background-radius: 4px;");
            PauseTransition pause = new PauseTransition(Duration.millis(500));
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
            
            contentLabel = new Label("Vacío");
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-font-style: italic;");
            HBox.setHgrow(contentLabel, Priority.ALWAYS);
            
            getChildren().addAll(frameLabel, colorIndicator, contentLabel);
        }
        
        public void load(String pid, int page, Color processColor) {
            occupied = true;
            contentLabel.setText(pid + " (P" + page + ")");
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
            colorIndicator.setFill(processColor);
            setStyle("-fx-background-color: rgba(0,255,136,0.1); -fx-background-radius: 6px; -fx-border-color: " + toRgbString(processColor) + "; -fx-border-radius: 6px; -fx-border-width: 2px;");
        }
        
        public void evict() {
            occupied = false;
            contentLabel.setText("Vacío");
            contentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-font-style: italic;");
            colorIndicator.setFill(Color.web("#333333"));
            setStyle("-fx-background-color: rgba(26,16,43,0.6); -fx-background-radius: 6px; -fx-border-color: #333; -fx-border-radius: 6px;");
        }

        public void highlightHit() {
            String currentStyle = getStyle();
            setStyle(currentStyle + "; -fx-background-color: rgba(0,255,136,0.3);");
            PauseTransition pause = new PauseTransition(Duration.millis(400));
            pause.setOnFinished(e -> setStyle(currentStyle));
            pause.play();
        }
        
        public void highlightVictim() {
            String currentStyle = getStyle();
            setStyle(currentStyle + "; -fx-border-color: #ffe066; -fx-border-width: 3px;");
            PauseTransition pause = new PauseTransition(Duration.millis(800));
            pause.setOnFinished(e -> setStyle(currentStyle));
            pause.play();
        }
        
        public void flashFault() {
            String currentStyle = getStyle();
            setStyle(currentStyle + "; -fx-background-color: rgba(255,79,109,0.2);");
            PauseTransition pause = new PauseTransition(Duration.millis(300));
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