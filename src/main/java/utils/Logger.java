package utils;

import model.ProcessState;
import modules.memory.MemoryManager.Frame;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Logger {

    // Interfaz para que los paneles se iluminen cuando se emita un log
    public interface PanelHighlightListener {
        void onLogEmitted(LogLevel level);
    }

    private static final List<LogEntry> logs = new ArrayList<>();
    private static boolean enableConsoleOutput = true;
    private static boolean enableFileOutput = false;
    private static String logFilePath = "simulation.log";
    private static final List<Consumer<LogEntry>> listeners = new ArrayList<>();
    private static final List<PanelHighlightListener> panelListeners = new ArrayList<>();

    public static class LogEntry {
        private final String message;
        private final LogLevel level;

        public LogEntry(String message, LogLevel level) {
            this.message = message;
            this.level = level;
        }

        @Override
        public String toString() {
            return String.format("%s", message);
        }

        public String getMessage() {
            return message;
        }

        public LogLevel getLevel() {
            return level;
        }

    }

    // Comunicacion con la interfaz
    public static void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
    }

    private static void notifyListeners(LogEntry entry) {
        for (Consumer<LogEntry> listener : listeners) {
            listener.accept(entry);
        }
    }

    // Panel highlight listeners
    public static void addPanelListener(PanelHighlightListener listener) {
        panelListeners.add(listener);
    }

    public static void removePanelListener(PanelHighlightListener listener) {
        panelListeners.remove(listener);
    }

    private static void notifyPanelListeners(LogLevel level) {
        for (PanelHighlightListener listener : panelListeners) {
            listener.onLogEmitted(level);
        }
    }

    // Niveles de log
    public enum LogLevel {
        INFO,
        WARNING,
        ERROR,
        DEBUG,
        EVENT,
        MEM,
        EXE,
        SYNC,
        PROC,

    }

    public static void log(String message) {
        log(message, LogLevel.INFO);
    }

    // Log de un nivel
    public static void log(String message, LogLevel level) {
        LogEntry entry = new LogEntry(message, level);
        logs.add(entry);

        if (enableConsoleOutput) {
            System.out.println(entry);
        }

        if (enableFileOutput) {
            appendToFile(entry);
        }

        notifyListeners(entry);
        notifyPanelListeners(level); // Notificar a los paneles para que se iluminen
    }

    // Log para cambio de estado de un proceso sergio
    public static void logStateChange(String pid, ProcessState oldState,
            ProcessState newState, int time) {
        String message = String.format(
                "[T=%d] Proceso %s: %s → %s",
                time, pid, oldState, newState);
        log(message, LogLevel.PROC);
    }

    // Log de ejecucion de una rafaga
    public static void logBurstExecution(String pid, String burstType,
            int duration, int time) {
        String message = String.format(
                "[T=%d] Proceso %s ejecutando rafaga %s por %d unidades",
                time, pid, burstType, duration);
        log(message, LogLevel.EXE);
    }

    public static void error(String message) {
        log(message, LogLevel.ERROR);
    }

    public static void warning(String message) {
        log(message, LogLevel.WARNING);
    }

    public static void debug(String message) {
        log(message, LogLevel.DEBUG);
    }

    // Para tipos
    public static void memLog(String message) {
        log(message, LogLevel.MEM);
    }

    public static void syncLog(String message) {
        log(message, LogLevel.SYNC);
    }

    public static void procLog(String message) {
        log(message, LogLevel.PROC);
    }

    public static void exeLog(String message) {
        log(message, LogLevel.EXE);
    }

    public static List<LogEntry> getAllLogs() {
        return new ArrayList<>(logs);
    }

    public static List<LogEntry> getLogsByLevel(LogLevel level) {
        return logs.stream()
                .filter(entry -> entry.getLevel() == level)
                .toList();
    }

    public static void clear() {
        logs.clear();
        log("Log limpiado");
    }

    public static void exportToFile(String filepath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.println("Generado: " + LocalDateTime.now());
            writer.println("Total de entradas: " + logs.size());
            writer.println();

            for (LogEntry entry : logs) {
                writer.println(entry);
            }

            writer.println();
        }
        log("Logs exportados a: " + filepath);
    }

    private static void appendToFile(LogEntry entry) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println(entry);
        } catch (IOException e) {
            System.err.println("Error escribiendo log: " + e.getMessage());
        }
    }

    // Resumen de logs
    public static String getSummary() {
        long infoCount = logs.stream().filter(e -> e.level == LogLevel.INFO).count();
        long warningCount = logs.stream().filter(e -> e.level == LogLevel.WARNING).count();
        long errorCount = logs.stream().filter(e -> e.level == LogLevel.ERROR).count();
        long debugCount = logs.stream().filter(e -> e.level == LogLevel.DEBUG).count();
        long eventCount = logs.stream().filter(e -> e.level == LogLevel.EVENT).count();

        return String.format(
                "Resumen de Logs:\n" +
                        "  Total: %d entradas\n" +
                        "  INFO: %d\n" +
                        "  WARNING: %d\n" +
                        "  ERROR: %d\n" +
                        "  DEBUG: %d\n" +
                        "  EVENT: %d",
                logs.size(), infoCount, warningCount, errorCount, debugCount, eventCount);
    }

    public static void printSummary() {
        System.out.println(getSummary());
    }

    public static void setEnableConsoleOutput(boolean enable) {
        enableConsoleOutput = enable;
    }

    public static void setEnableFileOutput(boolean enable) {
        enableFileOutput = enable;
    }

    public static void setLogFilePath(String path) {
        logFilePath = path;
    }

    /// memoria
    public static void memLoad(String pid, int page, int frame, int time) {
        log(String.format(
                "[T=%d][MEM][LOAD] Se carga la pagina %d del proceso %s en el marco %d",
                time, page, pid, frame), LogLevel.MEM);
    }

    public static void memHit(String pid, int page, int frame, int time) {
        log(String.format(
                "[T=%d][MEM][HIT] La pagina %d del proceso %s ya estaba cargada (marco %d)",
                time, page, pid, frame), LogLevel.MEM);
    }

    public static void memFault(String pid, int page, int time) {
        log(String.format(
                "[T=%d][MEM][PAGE FAULT] El proceso %s pidio la pagina %d, pero NO estaba en memoria",
                time, pid, page), LogLevel.MEM);
    }

    public static void memReplace(String oldPid, int oldPage, String newPid, int newPage, int frame, String reason, int time) {
        log(String.format(
                "[T=%d][MEM][REPLACE] Se reemplazo %s:P%d por %s:P%d en el marco %d | Motivo: %s",
                time, oldPid, oldPage, newPid, newPage, frame, reason), LogLevel.MEM);
    }
    //Este si se imprime
    public static void memSnapshot(Frame[] frames) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nMEMORIA FISICA ____________________________________\n");
        for (int i = 0; i < frames.length; i++) {
            Frame f = frames[i];
            if (!f.isOccupied()) {
                sb.append(String.format("  Frame %d : [LIBRE]\n", i));
            } else {
                sb.append(String.format(
                        "  Frame %d : Proceso %s | Pagina %d | R=%b | M=%b\n",
                        i, f.getProcessId(), f.getPageNumber(), f.isReferenced(), f.isModified()));
            }
        }
        sb.append("_____________________________________________________\n");
        log(sb.toString(), LogLevel.MEM);
    }
}
