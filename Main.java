import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.FCFS;
import modules.scheduler.Scheduler;
import modules.sync.SyncController;
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
            SyncController syncController = new SyncController(scheduler, memoryManager);
            
            // Mostrar resumen de configuración
            printConfiguration(config, processes);
            
            // Ejecutar simulación básica (modo consola)
            Logger.section("INICIANDO SIMULACIÓN");
            runBasicSimulation(processes, scheduler, memoryManager, syncController);
            
            // Mostrar resultados
            Logger.section("RESULTADOS DE LA SIMULACIÓN");
            scheduler.printMetrics();
            memoryManager.printMetrics();
            
            Logger.section("SIMULACIÓN COMPLETADA");
            Logger.printSummary();
            
        } catch (Exception e) {
            Logger.error("Error fatal: " + e.getMessage());
            e.printStackTrace();
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
        };
    }
    
    /**
     * Crea el gestor de memoria según la configuración
     */
    private static MemoryManager createMemoryManager(Config config) {
      int frames = config.getTotalFrames();
      
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
      };
    }
    
    /**
     * Simulación básica sin threads (para probar la infraestructura)
     */
    private static void runBasicSimulation(List<Process> processes, 
                                          Scheduler scheduler,
                                          MemoryManager memoryManager,
                                          SyncController syncController) {
        
        Logger.log("Ejecutando simulación básica (sin concurrencia)");
        
        int currentTime = 0;
        int maxTime = 1000; // Límite de seguridad
        
        // Añadir procesos que llegaron al tiempo 0
        for (Process p : processes) {
            if (p.getArrivalTime() == 0) {
                p.setState(model.ProcessState.READY);
                scheduler.addProcess(p);
                Logger.log("Proceso " + p.getPid() + " llegó al sistema");
            }
        }
        
        while (currentTime < maxTime) {
            syncController.synchronizeTime(currentTime);
            
            // Verificar nuevas llegadas
            for (Process p : processes) {
                if (p.getArrivalTime() == currentTime && 
                    p.getState() == model.ProcessState.NEW) {
                    p.setState(model.ProcessState.READY);
                    scheduler.addProcess(p);
                    Logger.log("Proceso " + p.getPid() + " llegó al sistema en t=" + currentTime);
                }
            }
            
            // Seleccionar siguiente proceso
            Process current = scheduler.selectNextProcess();
            
            if (current == null) {
                // No hay procesos listos
                if (allProcessesFinished(processes)) {
                    Logger.log("Todos los procesos han finalizado");
                    break;
                }
                scheduler.recordIdleTime(1);
                currentTime++;
                continue;
            }
            
            // Preparar proceso para ejecución
            if (!syncController.prepareProcessForExecution(current)) {
                Logger.warning("No se pudo preparar proceso " + current.getPid());
                currentTime++;
                continue;
            }
            
            // Ejecutar ráfaga
            model.Burst burst = current.getCurrentBurst();
            if (burst != null) {
                current.markFirstExecution(currentTime);
                
                int executionTime = Math.min(burst.getRemainingTime(), 1);
                burst.execute(executionTime);
                scheduler.recordCPUTime(executionTime);
                
                Logger.debug("Ejecutando " + current.getPid() + " - " + 
                           burst.getType() + " por " + executionTime + " unidad(es)");
                
                if (burst.isCompleted()) {
                    current.advanceBurst();
                    
                    if (current.isCompleted()) {
                        current.setCompletionTime(currentTime + 1);
                        syncController.releaseProcessResources(current);
                    } else {
                        // Volver a ready para siguiente ráfaga
                        scheduler.addProcess(current);
                    }
                } else {
                    // Ráfaga no completada, volver a ready
                    scheduler.addProcess(current);
                }
            }
            
            currentTime++;
        }
        
        if (currentTime >= maxTime) {
            Logger.warning("Simulación detenida por límite de tiempo");
        }
    }
    
    private static boolean allProcessesFinished(List<Process> processes) {
        return processes.stream()
            .allMatch(p -> p.getState() == model.ProcessState.TERMINATED);
    }
    
    private static void printConfiguration(Config config, List<Process> processes) {
        Logger.separator();
        Logger.log("CONFIGURACIÓN DEL SISTEMA:");
        Logger.log("  Algoritmo de planificación: " + config.getSchedulerType());
        Logger.log("  Algoritmo de reemplazo: " + config.getReplacementType());
        Logger.log("  Marcos de memoria: " + config.getTotalFrames());
        Logger.log("  Quantum (RR): " + config.getQuantum());
        Logger.log("  E/S habilitada: " + config.isEnableIO());
        Logger.log("  Número de procesos: " + processes.size());
        Logger.separator();
    }
    
    private static void printUsage() {
        System.out.println("USO: java Main <archivo_config> <archivo_procesos>");
        System.out.println();
        System.out.println("Ejemplo:");
        System.out.println("  java Main data/config.txt data/procesos.txt");
        System.out.println();
        System.out.println("Los archivos deben existir y ser legibles.");
    }
}