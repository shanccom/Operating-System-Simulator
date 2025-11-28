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
  
  private final Object engineMonitor = new Object();
  
  private int currentTime;
  private volatile boolean running;
  
  public SimulationEngine(Scheduler scheduler, MemoryManager memoryManager, List<Process> processes, Config config) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.syncController = new SyncController(scheduler, memoryManager);
    this.ioManager = new IOManager(syncController);
    this.allProcesses = processes;
    this.config = config;
    this.currentTime = 0;
    this.running = false;
    
    // Crear un thread independiente por cada proceso
    this.processThreads = new ArrayList<>();
    for (Process process : processes) {
      ProcessThread thread = new ProcessThread(process, syncController, ioManager);
      processThreads.add(thread);
    }
  }
  
  public void run() {
    synchronized(engineMonitor) {
      running = true;
    }
    
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
    ioManager.start();
    sleep(50);
    for (ProcessThread thread : processThreads) {
      thread.start();
      Logger.log("  Thread creado: " + thread.getName());
      sleep(10);
    }
  }

  private void notifyTimeAdvance() {
    synchronized(engineMonitor) {
      // Despertar todos los threads de procesos para que verifiquen su tiempo de llegada
      for (ProcessThread thread : processThreads) {
        Process process = thread.getProcess();
        synchronized(process) {
          process.notifyAll();
        }
      }
    }
  }
  
  private void coordinationLoop() { 
    while (isRunning() && !allProcessesCompleted()) {
      notifyTimeAdvance();
      
      // Incrementar tiempo de espera de procesos READY
      synchronized(engineMonitor) {
        for (Process p : allProcesses) {
          if (p.getState() == ProcessState.READY) {
            p.incrementWaitingTime();
          }
        }
      }

      // Coordinar con el planificador para seleccionar proceso
      coordinateScheduler();
      
      if (getCurrentTime() % 5 == 0) {
        printSystemState();
      }
      
      // Sincronizar tiempo global
      syncController.synchronizeTime(getCurrentTime());
      
      synchronized(engineMonitor) {
        currentTime++;
      }
      
      scheduler.incrementTime();
      sleep(config.getTimeUnit());
    }
    
    Logger.log("BUCLE DE COORDINACIÓN TERMINADO");
  }
  
  private void coordinateScheduler() {
    Process nextProcess = scheduler.selectNextProcess();
    
    if (nextProcess == null) {
        Logger.debug("[MOTOR → SCHEDULER] No hay procesos listos → CPU IDLE");
        scheduler.recordIdleTime(1);
        return;
    }
    
    Process currentRunning = scheduler.getCurrentProcess();
    boolean isSameProcess = (currentRunning != null && 
                             currentRunning == nextProcess && 
                             nextProcess.getState() == ProcessState.RUNNING);
    
    boolean canExecute;
    
    if (isSameProcess) {
        // El proceso ya está ejecutando, no necesita preparación
        Logger.debug("[MOTOR] Proceso " + nextProcess.getPid() + " continúa ejecutando");
        canExecute = true;
    } else {
        // Es un proceso nuevo, necesita preparación (cargar páginas, etc.)
        canExecute = syncController.prepareProcessForExecution(nextProcess);
        
        if (canExecute) {
            Logger.log("[MOTOR → SYNC] Proceso preparado");
            wakeUpThread(nextProcess);
        } else {
            Logger.log("[MOTOR → SYNC] Proceso bloqueado");
        }
    }
    
    if (canExecute) {
        scheduler.recordCPUTime(1);
    } else {
        scheduler.recordIdleTime(1);
    }
  } 
  
  private void wakeUpThread(Process process) {
    synchronized(engineMonitor) {
      for (ProcessThread thread : processThreads) {
        if (thread.getProcess() == process) {
          Logger.log("[MOTOR → THREAD-" + process.getPid() + "] Despertando thread para ejecutar");
          thread.wakeUp();
          break;
        }
      }
    }
  }
  
  private void waitForAllThreads() {
    Logger.log("[MOTOR] Esperando finalización de todos los threads...");
    
    for (ProcessThread thread : processThreads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Logger.error("Error esperando thread: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    
    Logger.log("[MOTOR] ✓ Todos los threads han finalizad");
  }
  
  private boolean allProcessesCompleted() {
    synchronized(engineMonitor) {
      return allProcesses.stream().allMatch(p -> p.getState() == ProcessState.TERMINATED);
    }
  }
  
  private void printSystemState() {
    Logger.log("ESTADO DEL SISTEMA (t=" + getCurrentTime() + ")");
    
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
    
    synchronized(engineMonitor) {
      long blocked = allProcesses.stream().filter(p -> p.getState().isBlocked()).count();
      Logger.log("BLOQUEADOS: " + blocked + " procesos");
    }
    
    Logger.log("MEMORIA: " + memoryManager.getFreeFrames() + "/" + memoryManager.getTotalFrames() + " marcos libres");
  }
  
  private void showResults() {
    scheduler.printMetrics();
    memoryManager.printMetrics();
    
    Logger.log("MÉTRICAS POR PROCESO");
    
    synchronized(engineMonitor) {
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
    }
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
    synchronized(engineMonitor) {
      running = false;
    }
    
    for (ProcessThread thread : processThreads) {
      thread.stopThread();
    }
  }
  
  public synchronized int getCurrentTime() { 
    return currentTime; 
  }
  
  public synchronized boolean isRunning() { 
    return running; 
  }
  
  public SyncController getSyncController() { 
    return syncController; 
  }
}