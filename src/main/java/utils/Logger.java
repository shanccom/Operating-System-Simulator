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
    
  private static final List<LogEntry> logs = new ArrayList<>();
  private static boolean enableConsoleOutput = true;
  private static boolean enableFileOutput = false;
  private static String logFilePath = "simulation.log";
  private static final List<Consumer<LogEntry>> listeners = new ArrayList<>();

  public static class LogEntry {
      private final LocalDateTime timestamp;
      private final String message;
      private final LogLevel level;
      
      public LogEntry(String message, LogLevel level) {
          this.timestamp = LocalDateTime.now();
          this.message = message;
          this.level = level;
      }
      
      @Override
      public String toString() {
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
          return String.format("[%s] %s", 
              timestamp.format(formatter), message);
      }
      
      public String getMessage() {
          return message;
      }
      
      public LogLevel getLevel() {
          return level;
      }
      
      public LocalDateTime getTimestamp() {
          return timestamp;
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
  // Niveles de log
  public enum LogLevel {
      INFO,
      WARNING,
      ERROR,
      DEBUG,
      EVENT
  }

  public static void log(String message) {
      log(message, LogLevel.INFO);
  }
  
  //Log de un nivel
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
  }
  
  //Log para cambio de estado de un proceso
  public static void logStateChange(String pid, ProcessState oldState, 
                                    ProcessState newState, int time) {
      String message = String.format(
          "[T=%d] Proceso %s: %s → %s",
          time, pid, oldState, newState
      );
      log(message, LogLevel.EVENT);
  }
  
  // Log para fallo de pagina
  public static void logPageFault(String pid, int pageNumber, int time) {
      String message = String.format(
          "[T=%d] PAGE FAULT - Proceso %s necesita SU pagina %d",
          time, pid, pageNumber
      );
      log(message, LogLevel.WARNING);
  }
  
  // Log de reemplazo de pagina
  public static void logPageReplacement(String victimPid, int victimPage,
                                        String newPid, int newPage, int time) {
      String message = String.format(
          "[T=%d] REEMPLAZO - Proceso %s pagina %d → Proceso %s pagina %d",
          time, victimPid, victimPage, newPid, newPage
      );
      log(message, LogLevel.EVENT);
  }
  
  // Log de ejecucion de una rafaga
  public static void logBurstExecution(String pid, String burstType, 
                                      int duration, int time) {
      String message = String.format(
          "[T=%d] Proceso %s ejecutando rafaga %s por %d unidades",
          time, pid, burstType, duration
      );
      log(message, LogLevel.DEBUG);
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
          logs.size(), infoCount, warningCount, errorCount, debugCount, eventCount
      );
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
    ///memoria
    public static void memLoad(String pid, int page, int frame) {
        log(String.format(
            "[MEM][LOAD] Se cargó la página %d del proceso %s en el marco %d",
            page, pid, frame
        ), LogLevel.EVENT);
    }

    public static void memHit(String pid, int page, int frame) {
        log(String.format(
            "[MEM][HIT] La página %d del proceso %s ya estaba cargada (marco %d)",
            page, pid, frame
        ), LogLevel.DEBUG);
    }

    public static void memFault(String pid, int page) {
        log(String.format(
            "[MEM][PAGE FAULT] El proceso %s pidio la página %d, pero NO estaba en memoria",
            pid, page
        ), LogLevel.WARNING);
    }

    public static void memReplace(String oldPid, int oldPage, String newPid, int newPage, int frame, String reason) {
        log(String.format(
            "[MEM][REPLACE] Se reemplazp %s:P%d por %s:P%d en el marco %d | Motivo: %s",
            oldPid, oldPage, newPid, newPage, frame, reason
        ), LogLevel.EVENT);
    }

    public static void memSnapshot(Frame[] frames) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nMEMORIA FISICA =====================================\n");
        for (int i = 0; i < frames.length; i++) {
            Frame f = frames[i];
            if (!f.isOccupied()) {
                sb.append(String.format("  Frame %d : [LIBRE]\n", i));
            } else {
                sb.append(String.format(
                    "  Frame %d : Proceso %s | Pagina %d | R=%b | M=%b\n",
                    i, f.getProcessId(), f.getPageNumber(), f.isReferenced(), f.isModified()
                ));
            }
        }
        sb.append("=====================================================\n");
        log(sb.toString(), LogLevel.DEBUG);
    }
}
