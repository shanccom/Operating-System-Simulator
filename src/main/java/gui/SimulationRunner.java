package gui;

import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import utils.FileParser;
import utils.Logger;
import utils.SimulationFactory;

import java.util.List;

public class SimulationRunner {

  public static void runSimulation(String configPath, String processPath) throws Exception {
      
    FileParser.validateFile(configPath);
    FileParser.validateFile(processPath);
    
    Logger.section("CARGANDO CONFIGURACIÓN");
    Config config = FileParser.parseConfig(configPath);
    
    if (!config.validate()) {
      throw new Exception("Configuración inválida");
    }
    
    Logger.log("Configuración válida: " + config);
    
    Logger.section("CARGANDO PROCESOS");
    List<Process> processes = FileParser.parseProcesses(processPath);
    
    if (processes.isEmpty()) {
      throw new Exception("No se encontraron procesos para simular");
    }
    
    Logger.log(processes.size() + " procesos cargados correctamente");
    
    Scheduler scheduler = SimulationFactory.createScheduler(config);
    MemoryManager memoryManager = SimulationFactory.createMemoryManager(config);
    
    printSystemConfiguration(config, processes, scheduler, memoryManager);
    Logger.section("INICIANDO SIMULACIÓN");
    SimulationEngine engine = new SimulationEngine(
      scheduler, memoryManager, processes, config
    );
    engine.run();
    Logger.printSummary();
  }
    
  private static void printSystemConfiguration(
          Config config, 
          List<Process> processes,
          Scheduler scheduler,
          MemoryManager memoryManager) {
      
    Logger.separator();
    Logger.log("CONFIGURACIÓN DEL SISTEMA:");
    Logger.log("  Algoritmo de planificación: " + scheduler.getAlgorithmName());
    Logger.log("  Algoritmo de reemplazo: " + memoryManager.getAlgorithmName());
    Logger.log("  Marcos de memoria: " + config.getTotalFrames());
    Logger.log("  Tamaño de marco: " + config.getFrameSize() + " bytes");
    Logger.log("  Quantum (RR): " + config.getQuantum() + " unidades");
    Logger.log("  E/S habilitada: " + (config.isEnableIO() ? "Sí" : "No"));
    Logger.log("  Unidad de tiempo: " + config.getTimeUnit() + " ms");
    Logger.log("  Número de procesos: " + processes.size());
    
    Logger.log("\nPROCESOS CARGADOS:");
    for (Process process : processes) {
      Logger.log(String.format(
        "  %s: Llegada=%d, Prioridad=%d, Ráfagas=%d, Memoria=%d páginas",
        process.getPid(),
        process.getArrivalTime(),
        process.getPriority(),
        process.getBursts().size(),
        process.getRequiredPages()
      ));
    }
    Logger.separator();
  }
}