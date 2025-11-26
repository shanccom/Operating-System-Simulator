import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import utils.FileParser;
import utils.Logger;
import utils.SimulationFactory;

import java.util.List;


public class Main {
    
  public static void main(String[] args) {
    try {
        
      String configFile = args[0];
      String processFile = args[1];
      
      FileParser.validateFile(configFile);
      FileParser.validateFile(processFile);
      
      Logger.section("CARGANDO CONFIGURACIÓN");
      Config config = FileParser.parseConfig(configFile);
      
      if (!config.validate()) {
        Logger.error("Configuración inválida");
        System.exit(1);
      }
      
      Logger.section("CARGANDO PROCESOS");
      List<Process> processes = FileParser.parseProcesses(processFile);
      
      if (processes.isEmpty()) {
        Logger.error("No se encontraron procesos para simular");
        System.exit(1);
      }
      
      Scheduler scheduler = SimulationFactory.createScheduler(config);
      MemoryManager memoryManager = SimulationFactory.createMemoryManager(config);
      
      printConfiguration(config, processes);
      
      SimulationEngine engine = new SimulationEngine( scheduler, memoryManager, processes, config
      );
      
      engine.run();
      Logger.printSummary();
        
    } catch (Exception e) {
      Logger.error("Error fatal: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
    
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
      
      Logger.log("\nPROCESOS CARGADOS:");
      for (Process process : processes) {
        Logger.log(String.format("  %s: Llegada=%d, Ráfagas=%d, Memoria=%d páginas",
          process.getPid(),
          process.getArrivalTime(),
          process.getBursts().size(),
          process.getRequiredPages()
        ));
      }
      Logger.separator();
    }
}