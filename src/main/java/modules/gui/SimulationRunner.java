package modules.gui;

import java.util.List;

import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import utils.FileParser;
import utils.Logger;
import utils.SimulationFactory;

public class SimulationRunner {

  public static void runSimulation(Config config, String processPath) throws Exception {
      
    if (!config.validate()) {
            throw new IllegalArgumentException("Configuración inválida");
    }
  
    List<Process> processes = FileParser.parseProcesses(processPath);
    if (processes.isEmpty()) {
      throw new Exception("No se encontraron procesos para simular");
    }
    Scheduler scheduler = SimulationFactory.createScheduler(config);
    MemoryManager memoryManager = SimulationFactory.createMemoryManager(config);
    printSystemConfiguration(config, processes, scheduler, memoryManager);
    System.out.println();
    SimulationEngine engine = new SimulationEngine(
      scheduler, memoryManager, processes, config
    );
    engine.run();
  }
    
  private static void printSystemConfiguration(Config config, List<Process> processes, Scheduler scheduler, MemoryManager memoryManager) {
    
    System.out.println();
    Logger.log("CONFIGURACION DEL SISTEMA:");
    Logger.log("Recuerda:  Cada proceso tiene sus propias paginas y no se mezclan, solo se comparte la memoria fisica(Frames)");
    //Una página solo necesita un frame libre donde colocarse. La tabla de páginas es la que guarda esa relación.
    Logger.log("    Algoritmo de planificacion: " + scheduler.getAlgorithmName());
    Logger.log("    Algoritmo de reemplazo: " + memoryManager.getAlgorithmName());
    Logger.log("    Marcos de memoria: " + config.getTotalFrames());
    Logger.log("    Tamaño de marco: " + config.getFrameSize() + " bytes");
    Logger.log("    Quantum (RR): " + config.getQuantum() + " unidades");
    Logger.log("    Unidad de tiempo: " + config.getTimeUnit() + " ms");
    Logger.log("    Numero de procesos: " + processes.size() + "\n");
    
    Logger.log("PROCESOS CARGADOS:");
    for (Process process : processes) {
      Logger.log(String.format(
        "   %s: Llegada = %d, Prioridad = %d, Rafagas = %d, Memoria = %d paginas",
        process.getPid(),
        process.getArrivalTime(),
        process.getPriority(),
        process.getBursts().size(),
        process.getRequiredPages()
      ));
    }
  }
}