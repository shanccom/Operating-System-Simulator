package utils;

import model.Config;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;

public class SimulationFactory {
    
  public static Scheduler createScheduler(Config config) {
    return switch (config.getSchedulerType()) {
      case FCFS -> new modules.scheduler.FCFS();
      case SJF -> new modules.scheduler.SJF();
      case SRT -> new modules.scheduler.SRT();
      case ROUND_ROBIN -> new modules.scheduler.RoundRobin(config.getQuantum());
      case PRIORITY -> new modules.scheduler.Priority();
      case PRIORITYPREEMPTIVE -> new modules.scheduler.PriorityPreemptive();
    };
  }
  
  public static MemoryManager createMemoryManager(Config config) {
    int frames = config.getTotalFrames();
    
    return switch (config.getReplacementType()) {
      case FIFO -> new modules.memory.FIFO(frames);
      case LRU -> new modules.memory.LRU(frames);
      case OPTIMAL -> new modules.memory.Optimal(frames);
      case NRU -> new modules.memory.NRU(frames);
    };
  }
}