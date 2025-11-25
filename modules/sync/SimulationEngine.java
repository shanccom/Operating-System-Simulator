package modules.sync;

import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;
import model.ProcessState;
import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {
  
  private final Scheduler scheduler;
  private final MemoryManager memoryManager;
  private final SyncController syncController;
  private final List<Process> allProcesses;
  private final IOManager ioManager;
  private final List<ProcessThread> processThreads;
  private final Config config;
  
  private int currentTime;
  private boolean running;
  
  public SimulationEngine(Scheduler scheduler, MemoryManager memoryManager, List<Process> processes, Config config) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.syncController = new SyncController(scheduler, memoryManager);
    this.ioManager = new IOManager(syncController);
    this.allProcesses = processes;
    this.config = config;
    this.currentTime = 0;
    this.running = false;
    
    this.processThreads = new ArrayList<>();
    for (Process process : processes) {
      ProcessThread thread = new ProcessThread(process, syncController, ioManager);
      processThreads.add(thread);
    }
    
    Logger.log("SimulationEngine creado con " + processes.size() + " procesos");
  }
  
  public void run() {
    Logger.log("Algoritmo planificación: " + scheduler.getAlgorithmName());
    Logger.log("Algoritmo memoria: " + memoryManager.getAlgorithmName());
    Logger.log("Total procesos: " + allProcesses.size());
    
    running = true;
    syncController.start();
    ioManager.start();
    startAllThreads();
    coordinationLoop();
    waitForAllThreads();
    ioManager.stop();
    syncController.stop();
    showResults();
  }
  
  private void startAllThreads() {
    Logger.log("Iniciando " + processThreads.size() + " threads de procesos...");
    
    for (ProcessThread thread : processThreads) {
      thread.start();
      Logger.debug("Thread iniciado: " + thread.getName());
    }
    
    Logger.log("Todos los threads iniciados");
  }

  private void notifyTimeAdvance() {
    for (ProcessThread thread : processThreads) {
      Process process = thread.getProcess();
      synchronized(process) {
        process.notifyAll();
      }
    }
  }
  
  private void coordinationLoop() {
      
    while (running && !allProcessesCompleted()) {
      notifyTimeAdvance();
      
      for (Process p : allProcesses) {
        if (p.getState() == ProcessState.READY) {
          p.incrementWaitingTime();
        }
      }

      if (currentTime % 5 == 0) {
        printSystemState();
      }
      
      syncController.synchronizeTime(currentTime);
      coordinateScheduler();
      currentTime++;
      scheduler.incrementTime();
      sleep(config.getTimeUnit());
    }
    
    Logger.log("Bucle de coordinación terminado");
  }
  
  private void coordinateScheduler() {
    Process nextProcess = scheduler.selectNextProcess();
    
    if (nextProcess == null) {
      Logger.debug("CPU IDLE - No hay procesos listos");
      scheduler.recordIdleTime(1);
      return;
    }
    
    boolean canExecute = syncController.prepareProcessForExecution(nextProcess);
    
    if (canExecute) {
      Logger.debug("Proceso " + nextProcess.getPid() + " seleccionado y preparado");
      wakeUpThread(nextProcess);
      scheduler.recordCPUTime(1);
    } else {
      Logger.debug("Proceso " + nextProcess.getPid() + " bloqueado");
    }
  }
  
  private void wakeUpThread(Process process) {
    for (ProcessThread thread : processThreads) {
      if (thread.getProcess() == process) {
        thread.wakeUp();
        break;
      }
    }
  }
  
  private void waitForAllThreads() {
    Logger.log("Esperando a que todos los threads terminen...");
    
    for (ProcessThread thread : processThreads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Logger.error("Error esperando thread: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    
    Logger.log("Todos los threads han terminado");
  }
  
  private boolean allProcessesCompleted() {
    return allProcesses.stream().allMatch(p -> p.getState() == model.ProcessState.TERMINATED);
  }
  
  private void printSystemState() {
    List<Process> readyQueue = scheduler.getReadyQueueSnapshot();
    Logger.log("Cola READY: " + readyQueue.size() + " procesos");
    for (Process p : readyQueue) {
      Logger.log("  • " + p.getPid() + " (Espera: " + p.getWaitingTime() + ")");
    }
    
    Process running = scheduler.getCurrentProcess();
    if (running != null) {
      model.Burst burst = running.getCurrentBurst();
      Logger.log("EJECUTANDO: " + running.getPid() + " - " + (burst != null ? burst.getType() : "NULL"));
    } else {
        Logger.log("EJECUTANDO: [CPU IDLE]");
    }
    
    long blocked = allProcesses.stream().filter(p -> p.getState().isBlocked()).count();
    Logger.log("BLOQUEADOS: " + blocked + " procesos");
    Logger.log("MEMORIA: " + memoryManager.getFreeFrames() + "/" + memoryManager.getTotalFrames() + " marcos libres");
  }
  
  private void showResults() {
    Logger.separator();
    scheduler.printMetrics();
    memoryManager.printMetrics();
    
    Logger.separator();
    Logger.section("MÉTRICAS POR PROCESO");
    for (Process p : allProcesses) {
      Logger.log(String.format(
        "%s: Espera=%d, Retorno=%d, Respuesta=%d, PageFaults=%d",
        p.getPid(),
        p.getWaitingTime(),
        p.getTurnaroundTime(),
        p.getResponseTime(),
        p.getPageFaults()
      ));
    }
    Logger.separator();
  }
  
  private void sleep(int ms) {
    if (ms > 0) {
      try {
        Thread.sleep(ms);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public void stop() {
    running = false;
    for (ProcessThread thread : processThreads) {
      thread.stopThread();
    }
  }
  
  public int getCurrentTime() { return currentTime; }
  public boolean isRunning() { return running; }
  public SyncController getSyncController() { return syncController; }
}