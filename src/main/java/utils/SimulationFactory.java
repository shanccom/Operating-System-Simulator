package utils;

import model.Config;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.scheduler.FCFS;

public class SimulationFactory {
    
  public static Scheduler createScheduler(Config config) {
    Logger.debug("Creando scheduler: " + config.getSchedulerType());
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
    
  public static MemoryManager createMemoryManager(Config config) {
    int frames = config.getTotalFrames();
    if (frames <= 0) {
      Logger.warning("Número de marcos inválido, usando 10 por defecto");
      frames = 10;
    }
    Logger.debug("Creando memory manager: " + config.getReplacementType() + 
                " con " + frames + " marcos");
    
    return switch (config.getReplacementType()) {
      case FIFO -> {
        Logger.log("Inicializando FIFO con " + frames + " marcos");
        yield new modules.memory.FIFO(frames);
      }
      
      case LRU -> {
        Logger.log("Inicializando LRU con " + frames + " marcos");
        yield new modules.memory.LRU(frames);
      }
      
      case OPTIMAL -> {
        Logger.log("Inicializando OPTIMAL con " + frames + " marcos");
        yield new modules.memory.Optimal(frames);
      }
      
      case NRU -> {
        Logger.log("Inicializando NRU con " + frames + " marcos");
        yield new modules.memory.NRU(frames);
      }
      
      default -> {
        Logger.warning("Algoritmo de memoria desconocido, usando FIFO por defecto");
        yield new modules.memory.FIFO(frames);
      }
    };
  }
}