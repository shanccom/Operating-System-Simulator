package modules.sync;

import model.Config;
import model.Process;
import model.ResultadoProceso;
import model.DatosResultados;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;
import model.ProcessState;
import java.util.Map;
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
  //Objeto que contendra los resultados finales
  private DatosResultados datosFinales;

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
      ProcessThread thread = new ProcessThread(process, syncController, ioManager, config); // AÑADIR config
      processThreads.add(thread);
    }
  }
  /* 
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
  }*/
  
  private DatosResultados construirResultados() {
    Map<String, Integer> reemplazos = memoryManager.getReemplazosPorProceso();
    List<ResultadoProceso> resumen = new ArrayList<>();

    for (Process p : allProcesses) {
      resumen.add(new ResultadoProceso(p.getPid(),p.getWaitingTime(),p.getTurnaroundTime(),p.getResponseTime(),p.getPageFaults(),reemplazos.getOrDefault(p.getPid(), 0)));
    }

    return new DatosResultados(
        scheduler.getAverageWaitingTime(),
        scheduler.getAverageTurnaroundTime(),
        scheduler.getAverageResponseTime(),
        scheduler.getCPUUtilization(),
        memoryManager.getPageFaults(),
        memoryManager.getPageReplacements(),
        scheduler.getTotalCPUTime(),
        scheduler.getIdleTime(),
        memoryManager.getTotalFrames(),
        memoryManager.getFreeFrames(),
        resumen
    );
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

      synchronized(engineMonitor) {
        for (ProcessThread thread : processThreads) {
          Process p = thread.getProcess();
          if (p.getState() == ProcessState.BLOCKED_IO) {
            synchronized(p) {
              p.notifyAll(); // Despertar para que verifiquen si su I/O terminó
            }
          }
        }
      }

      if (getCurrentTime() % 5 == 0) {
        printSystemState();
      }
      
      // Sincronizar tiempo global
      syncController.synchronizeTime(getCurrentTime());
      
      synchronized(engineMonitor) {
        currentTime++;
        scheduler.setCurrentTime(currentTime);
      }
      
      sleep(config.getTimeUnit());
    }
  }

  private void coordinateScheduler() {
    Process nextProcess = scheduler.selectNextProcess();
    
    if (nextProcess == null) {
        scheduler.recordIdleTime(1);
        return;
    }
    
    Process currentRunning = scheduler.getCurrentProcess();
    
    // Context switch necesario
    if (currentRunning != nextProcess) {
        if (currentRunning != null && currentRunning.getState() == ProcessState.RUNNING) {
            synchronized(syncController.getCoordinationMonitor()) {
                currentRunning.setState(ProcessState.READY);
            }
        }
        
        // Preparar nuevo proceso
        boolean canExecute = syncController.prepareProcessForExecution(nextProcess);
        
        if (canExecute) {
            scheduler.confirmProcessSelection(nextProcess); 
            wakeUpThread(nextProcess);
            scheduler.recordCPUTime(1);
        } else {
            scheduler.recordIdleTime(1);
        }
    } 
    // El mismo proceso continúa
    else {
        if (nextProcess.getState() == ProcessState.RUNNING) {
            Logger.log("FCFS continúa con: " + nextProcess.getPid());
            wakeUpThread(nextProcess);
            scheduler.recordCPUTime(1);
        } else {
            boolean canExecute = syncController.prepareProcessForExecution(nextProcess);
            
            if (canExecute) {
                wakeUpThread(nextProcess);
                scheduler.recordCPUTime(1);
            } else {
                scheduler.recordIdleTime(1);
            }
        }
    }
  }
  
  private void wakeUpThread(Process process) {
    synchronized(engineMonitor) {
      for (ProcessThread thread : processThreads) {
        if (thread.getProcess() == process) {
          thread.wakeUp();
          break;
        }
      }
    }
  }
  
  private void waitForAllThreads() {
    for (ProcessThread thread : processThreads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Logger.error("Error esperando thread: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
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
      Logger.log("     " + p.getPid() + " (Espera: " + p.getWaitingTime() + ")");
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