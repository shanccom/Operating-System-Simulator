import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.FCFS;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import utils.FileParser;
import utils.Logger;
import modules.memory.*;
import java.util.List;

public class Main {
    
  public static void main(String[] args) {
    try {
      String configFile = args[0];
      String processFile = args[1];
      
      FileParser.validateFile(configFile);
      FileParser.validateFile(processFile);
      
      Logger.section("CARGANDO CONFIGURACION");
      Config config = FileParser.parseConfig(configFile);
      
      if (!config.validate()) {
        Logger.error("Configuracion invalida");
        return;
      }
      
      Logger.section("CARGANDO PROCESOS");
      List<Process> processes = FileParser.parseProcesses(processFile);
      
      if (processes.isEmpty()) {
        Logger.error("No se encontraron procesos para simular");
        return;
      }
       
      Scheduler scheduler = createScheduler(config);
      MemoryManager memoryManager = createMemoryManager(config);
      
      printConfiguration(config, processes);
      
      SimulationEngine engine = new SimulationEngine( scheduler, memoryManager, processes, config );
      
      engine.run();
      Logger.printSummary();
        
    } catch (Exception e) {
      Logger.error("Error fatal: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
    
  private static Scheduler createScheduler(Config config) {
    return switch (config.getSchedulerType()) {
      case FCFS -> new FCFS();
      case SJF -> { 
        Logger.warning("SJF no implementado aun, usando FCFS");
        yield new FCFS();
      }
      case ROUND_ROBIN -> {
        Logger.warning("Round Robin no implementado aun, usando FCFS");
        yield new FCFS();
      }
      case PRIORITY -> {
        Logger.warning("Priority no implementado aun, usando FCFS");
        yield new FCFS();
      }
      default -> {
        Logger.warning("Algoritmo desconocido, usando FCFS por defecto");
        yield new FCFS();
      }
    };
  }
    
  private static MemoryManager createMemoryManager(Config config) {
    int frames = config.getTotalFrames();
    
    if (frames <= 0) {
      Logger.warning("Numero de marcos invalido, usando 10 por defecto");
      frames = 10;
    }
    
    return switch (config.getReplacementType()) {
      case FIFO -> new modules.memory.FIFO(frames);
      case LRU -> new modules.memory.LRU(frames);
      case OPTIMAL -> new modules.memory.Optimal(frames);
      case NRU -> new modules.memory.NRU(frames);
      default -> {
        Logger.warning("Algoritmo de memoria desconocido, usando FIFO por defecto");
        yield new modules.memory.FIFO(frames);
      }
    };
  }
    
//Aun no se usa
  private static void printConfiguration(Config config, List<Process> processes) {
    Logger.separator();
    Logger.log("CONFIGURACION DEL SISTEMA:");
    Logger.log("  Algoritmo de planificacion: " + config.getSchedulerType());
    Logger.log("  Algoritmo de reemplazo: " + config.getReplacementType());
    Logger.log("  Marcos de memoria: " + config.getTotalFrames());
    Logger.log("  Quantum (RR): " + config.getQuantum());
    Logger.log("  E/S habilitada: " + config.isEnableIO());
    Logger.log("  Unidad de tiempo (ms): " + config.getTimeUnit());
    Logger.log("  Numero de procesos: " + processes.size());
    
    Logger.log("\nPROCESOS CARGADOS:");
    for (Process process : processes) {
      Logger.log(String.format("  %s: Llegada=%d, Rafagas=%d, Memoria=%d paginas",
        process.getPid(),
        process.getArrivalTime(),
        process.getBursts().size(),
        process.getRequiredPages()
      ));
    }
    Logger.separator();
  }
}