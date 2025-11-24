import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.FCFS;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import utils.FileParser;
import utils.Logger;

import java.util.List;

public class Main {
    
    public static void main(String[] args) {
        Logger.section("SIMULADOR DE SISTEMA OPERATIVO");
        Logger.log("Iniciando simulador...");
        
        try {
            // Validar argumentos
            if (args.length < 2) {
                printUsage();
                return;
            }
            
            String configFile = args[0];
            String processFile = args[1];
            
            // Validar archivos
            FileParser.validateFile(configFile);
            FileParser.validateFile(processFile);
            
            // Cargar configuración
            Logger.section("CARGANDO CONFIGURACIÓN");
            Config config = FileParser.parseConfig(configFile);
            
            if (!config.validate()) {
                Logger.error("Configuración inválida");
                return;
            }
            
            // Cargar procesos
            Logger.section("CARGANDO PROCESOS");
            List<Process> processes = FileParser.parseProcesses(processFile);
            
            if (processes.isEmpty()) {
                Logger.error("No se encontraron procesos para simular");
                return;
            }
            
            // Crear componentes del sistema
            Logger.section("INICIALIZANDO COMPONENTES");
            
            Scheduler scheduler = createScheduler(config);
            MemoryManager memoryManager = createMemoryManager(config);
            
            // Mostrar resumen de configuración
            printConfiguration(config, processes);
            
            // 🚀 NUEVO: Usar SimulationEngine en lugar de runBasicSimulation
            Logger.section("INICIANDO SIMULACIÓN AVANZADA");
            SimulationEngine engine = new SimulationEngine(
                scheduler, memoryManager, processes, config
            );
            engine.run();  // ← Esto ejecuta TODA la simulación
            
            Logger.section("SIMULACIÓN COMPLETADA");
            Logger.printSummary();
            
        } catch (Exception e) {
            Logger.error("Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Crea el planificador según la configuración
     */
    private static Scheduler createScheduler(Config config) {
        return switch (config.getSchedulerType()) {
            case FCFS -> new FCFS();
            case SJF -> {
                Logger.warning("SJF no implementado aún, usando FCFS");
                yield new FCFS();
            }
            case ROUND_ROBIN -> {
                Logger.warning("Round Robin no implementado aún, usando FCFS");
                yield new FCFS();
            }
            case PRIORITY -> {
                Logger.warning("Priority no implementado aún, usando FCFS");
                yield new FCFS();
            }
            default -> {
                Logger.warning("Algoritmo desconocido, usando FCFS por defecto");
                yield new FCFS();
            }
        };
    }
    
    /**
     * Crea el gestor de memoria según la configuración
     */
    private static MemoryManager createMemoryManager(Config config) {
        int frames = config.getTotalFrames();
        
        // Validar que hay suficientes marcos
        if (frames <= 0) {
            Logger.warning("Número de marcos inválido, usando 10 por defecto");
            frames = 10;
        }
        
        return switch (config.getReplacementType()) {
            case FIFO -> new modules.memory.FIFO(frames);
            case LRU -> {
                Logger.warning("LRU no implementado aún, usando FIFO");
                yield new modules.memory.FIFO(frames);
            }
            case OPTIMAL -> {
                Logger.warning("OPTIMAL no implementado aún, usando FIFO");
                yield new modules.memory.FIFO(frames);
            }
            default -> {
                Logger.warning("Algoritmo de memoria desconocido, usando FIFO por defecto");
                yield new modules.memory.FIFO(frames);
            }
        };
    }
    
    /**
     * Muestra la configuración del sistema
     */
    private static void printConfiguration(Config config, List<Process> processes) {
        Logger.separator();
        Logger.log("CONFIGURACIÓN DEL SISTEMA:");
        Logger.log("  Algoritmo de planificación: " + config.getSchedulerType());
        Logger.log("  Algoritmo de reemplazo: " + config.getReplacementType());
        Logger.log("  Marcos de memoria: " + config.getTotalFrames());
        Logger.log("  Quantum (RR): " + config.getQuantum());
        Logger.log("  E/S habilitada: " + config.isEnableIO());
        Logger.log("  Unidad de tiempo (ms): " + config.getTimeUnit());
        Logger.log("  Número de procesos: " + processes.size());
        
        // Mostrar resumen de procesos
        Logger.log("\nPROCESOS CARGADOS:");
        for (Process process : processes) {
            Logger.log(String.format("  %s: Llegada=%d, Ráfagas=%d, Memoria=%d páginas",
                process.getPid(),
                process.getArrivalTime(),
                process.getBursts().size(),
                process.getMemoryPages()
            ));
        }
        Logger.separator();
    }
    
    /**
     * Muestra el uso correcto del programa
     */
    private static void printUsage() {
        System.out.println("USO: java Main <archivo_config> <archivo_procesos>");
        System.out.println();
        System.out.println("Argumentos:");
        System.out.println("  <archivo_config>    Archivo de configuración del sistema");
        System.out.println("  <archivo_procesos>  Archivo con la lista de procesos");
        System.out.println();
        System.out.println("Ejemplo:");
        System.out.println("  java Main data/config.txt data/procesos.txt");
        System.out.println();
        System.out.println("Los archivos deben existir y ser legibles.");
    }
}