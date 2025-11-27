package utils;

import model.Burst;
import model.Config;
import model.Process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileParser {
    
    // Patrones regex para parsear las ráfagas
    private static final Pattern BURST_PATTERN = Pattern.compile("(CPU|IO)\\((\\d+)\\)");
    
    public static Config parseConfig(String filepath) throws IOException {
        Config config = new Config();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Ignorar líneas vacías y comentarios
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                String[] parts = line.split("=");
                if (parts.length != 2) continue;
                
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();
                
                // Remover comentarios inline (después del valor)
                int commentIndex = value.indexOf('#');
                if (commentIndex != -1) {
                    value = value.substring(0, commentIndex).trim();
                }
                
                switch (key) {
                    case "frames":
                    case "totalframes":
                        config.setTotalFrames(Integer.parseInt(value));
                        break;
                    case "framesize":
                        config.setFrameSize(Integer.parseInt(value));
                        break;
                    case "scheduler":
                        config.setSchedulerType(parseSchedulerType(value));
                        break;
                    case "quantum":
                        config.setQuantum(Integer.parseInt(value));
                        break;
                    case "replacement":
                        config.setReplacementType(parseReplacementType(value));
                        break;
                    case "enableio":
                    case "io":
                        config.setEnableIO(Boolean.parseBoolean(value));
                        break;
                    case "timeunit":
                        config.setTimeUnit(Integer.parseInt(value));
                        break;
                }
            }
        }
        
        Logger.log("Configuración cargada: " + config);
        return config;
    }
    

     //Lee el archivo de procesos

    public static List<Process> parseProcesses(String filepath) throws IOException {
        List<Process> processes = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Ignorar líneas vacías y comentarios
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                try {
                    Process process = parseProcessLine(line);
                    processes.add(process);
                    Logger.debug("Proceso parseado: " + process);
                } catch (Exception e) {
                    Logger.error("Error en línea " + lineNumber + ": " + e.getMessage());
                    throw new IOException("Error parseando línea " + lineNumber + ": " + line, e);
                }
            }
        }
        
        Logger.log("Total de procesos cargados: " + processes.size());
        return processes;
    }
    

    private static Process parseProcessLine(String line) {
        String[] parts = line.split("\\s+");
        
        if (parts.length < 5) {
            throw new IllegalArgumentException(
                "Formato inválido. Se esperan 5 campos: PID ARRIVAL BURSTS PRIORITY PAGES"
            );
        }
        
        String pid = parts[0];
        int arrivalTime = Integer.parseInt(parts[1]);
        List<Burst> bursts = parseBursts(parts[2]);
        int priority = Integer.parseInt(parts[3]);
        int requiredPages = Integer.parseInt(parts[4]);
        
        return new Process(pid, arrivalTime, bursts, priority, requiredPages);
    }
    

    private static List<Burst> parseBursts(String burstsString) {
        List<Burst> bursts = new ArrayList<>();
        
        Matcher matcher = BURST_PATTERN.matcher(burstsString);
        
        while (matcher.find()) {
            String type = matcher.group(1);
            int duration = Integer.parseInt(matcher.group(2));
            
            Burst.BurstType burstType = type.equalsIgnoreCase("CPU") 
                ? Burst.BurstType.CPU 
                : Burst.BurstType.IO;
            
            bursts.add(new Burst(burstType, duration));
        }
        
        if (bursts.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron ráfagas válidas en: " + burstsString);
        }
        
        return bursts;
    }
    

    private static Config.SchedulerType parseSchedulerType(String value) {
        return switch (value.toUpperCase()) {
            case "FCFS" -> Config.SchedulerType.FCFS;
            case "SJF" -> Config.SchedulerType.SJF;
            case "RR", "ROUNDROBIN", "ROUND_ROBIN" -> Config.SchedulerType.ROUND_ROBIN;
            case "PRIORITY" -> Config.SchedulerType.PRIORITY;
            default -> throw new IllegalArgumentException("Tipo de scheduler desconocido: " + value);
        };
    }
    

    private static Config.ReplacementType parseReplacementType(String value) {
        return switch (value.toUpperCase()) {
            case "FIFO" -> Config.ReplacementType.FIFO;
            case "LRU" -> Config.ReplacementType.LRU;
            case "OPTIMAL", "OPT" -> Config.ReplacementType.OPTIMAL;
            case "NRU" -> Config.ReplacementType.NRU;
            default -> throw new IllegalArgumentException("Tipo de reemplazo desconocido: " + value);
        };
    }
    

    public static void validateFile(String filepath) throws IOException {
        java.io.File file = new java.io.File(filepath);
        if (!file.exists()) {
            throw new IOException("El archivo no existe: " + filepath);
        }
        if (!file.canRead()) {
            throw new IOException("No se puede leer el archivo: " + filepath);
        }
    }
}