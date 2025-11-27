package utils;

import model.Config;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.scheduler.FCFS;

public class SimulationFactory {
    
  public static Scheduler createScheduler(Config config) {
    return switch (config.getSchedulerType()) {
      case FCFS -> new FCFS();
      case SJF -> new SJF()
      case ROUND_ROBIN -> new ROUND_ROBIN()
      case PRIORITY -> new PRIORITY()
      default -> {
        Logger.warning("Algoritmo desconocido, usando FCFS por defecto");
        yield new FCFS();
      }
    };
  }
    
  public static MemoryManager createMemoryManager(Config config) {
    int frames = config.getTotalFrames();
    if (frames <= 0) {
      Logger.warning("Numero de marcos invalido, usando 10 por defecto");
      frames = 10;
    }
    return switch (config.getReplacementType()) {
      case FIFO -> {
        yield new modules.memory.FIFO(frames);
      }
      
      case LRU -> {
        yield new modules.memory.LRU(frames);
      }
      
      case OPTIMAL -> {
        yield new modules.memory.Optimal(frames);
      }
      
      case NRU -> {
        yield new modules.memory.NRU(frames);
      }
      
      default -> {
        Logger.warning("Algoritmo de memoria desconocido, usando FIFO por defecto");
        yield new modules.memory.FIFO(frames);
      }
    };
  }
}