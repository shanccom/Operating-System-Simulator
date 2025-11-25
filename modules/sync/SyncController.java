package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SyncController {
  
  private final Scheduler scheduler;
  private final MemoryManager memoryManager;
  
  private final Lock schedulerLock;
  private final Lock memoryLock;
  private final Lock globalLock;
  
  private final Condition memoryAvailable;
  private final Condition processReady;
  
  private volatile boolean running;
  private volatile boolean paused;
  
  public SyncController(Scheduler scheduler, MemoryManager memoryManager) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    
    this.schedulerLock = new ReentrantLock();
    this.memoryLock = new ReentrantLock();
    this.globalLock = new ReentrantLock();
    
    this.memoryAvailable = globalLock.newCondition();
    this.processReady = globalLock.newCondition();
    
    this.running = false;
    this.paused = false;
    
    Logger.log("SyncController inicializado");
  }
  
  public boolean prepareProcessForExecution(Process process) {
    globalLock.lock();
    try {
      Logger.log("[SYNC] Adquiriendo lock global para proceso " + process.getPid());
      Logger.debug("Preparando proceso " + process.getPid() + " para ejecucion");
      
      if (process.getState() != ProcessState.READY) {
        Logger.warning("Proceso " + process.getPid() + " no esta en estado READY");
        return false;
      }
      
      boolean pagesLoaded = loadRequiredPages(process);
      
      if (!pagesLoaded) {
        process.setState(ProcessState.BLOCKED_MEMORY);
        Logger.log("[SYNC] Proceso " + process.getPid() + " bloqueado esperando memoria");
        return false;
      }
      
      process.setState(ProcessState.RUNNING);
      return true;
        
    } finally {
      Logger.debug("[SYNC] Liberando lock global");
      globalLock.unlock();
    }
  }
  
  private int calculatePagesForBurst(Process process, Burst burst) {
    int totalPages = process.getRequiredPages();
    
    int basePages = Math.max(1, (int) Math.ceil(totalPages * 0.4));
    
    if (burst.isCPU()) {
        return Math.min(basePages, totalPages);
    } else {
        return Math.min(basePages + 1, totalPages);
    }
  }


  private boolean loadRequiredPages(Process process) {
    memoryLock.lock();
    try {
      Logger.debug("[SYNC] Adquiriendo lock de memoria para " + process.getPid());
      
      Burst currentBurst = process.getCurrentBurst();
      if (currentBurst == null){
        return false;
      }

      int requiredForBurst = calculatePagesForBurst(process, currentBurst);
      Logger.debug("Proceso " + process.getPid() + " necesita " + requiredForBurst + " paginas para la rafaga actual");

      int loadedCount = 0;
      for (int page = 0; page < requiredForBurst; page++) {
        if(!memoryManager.isPageLoaded(process.getPid(), page)) {
          Logger.debug("[SYNC] Esperando cargar página " + page + " con lock activo");
          boolean loaded = memoryManager.loadPage(process, page);

          if (loaded) {
            loadedCount++;
            Logger.debug("[SYNC] Página " + page + " cargada exitosamente");
          } else {
            Logger.warning("No se pudo cargar pagina " + page + " del proceso " + process.getPid());
                    return false; // Fallo critico
          }
        } else {
          loadedCount++;
        }
      }

      if (loadedCount >= requiredForBurst) {
        Logger.debug("Proceso " + process.getPid() + " tiene todas las paginas necesarias cargadas");
        return true;
      }
        return false;
    } finally {
      memoryLock.unlock();
    }
  }
  
  public void notifyProcessBlocked(Process process, ProcessState blockReason) {
    globalLock.lock();
    try {
      Logger.debug("[SYNC] Lock adquirido para notificar proceso " + process.getPid());
      ProcessState previousState = process.getState();

      if (previousState != ProcessState.READY) {
        Logger.logStateChange(process.getPid(), previousState, ProcessState.READY, scheduler.getCurrentTime());
        process.setState(ProcessState.READY);
      }

      Logger.log(">>> Agregando proceso " + process.getPid() + " a la cola del scheduler");
      scheduler.addProcess(process);
      Logger.debug("[SYNC] Señalando condición processReady"); 
      processReady.signalAll(); 
    } finally {
      Logger.debug("[SYNC] Liberando lock global");
      globalLock.unlock();
    }
  }
  
  public void notifyProcessReady(Process process) {
    globalLock.lock();

    try {
      ProcessState previousState = process.getState();

      if (previousState != ProcessState.READY) {
        Logger.logStateChange(process.getPid(), previousState, ProcessState.READY, scheduler.getCurrentTime());
        process.setState(ProcessState.READY);
      }

      Logger.log(">>> Agregando proceso " + process.getPid() + " a la cola del scheduler");
      scheduler.addProcess(process);
      processReady.signalAll();
      
    } finally {
      globalLock.unlock();
    }
  }
  
  public void waitForReadyProcess() throws InterruptedException {
    globalLock.lock();
    try {
      while (running && !scheduler.hasReadyProcesses()) {
          processReady.await();
      }
    } finally {
      globalLock.unlock();
    }
  }
  
  public void waitForMemoryAvailable() throws InterruptedException {
    globalLock.lock();
    try {
      while (running && memoryManager.getFreeFrames() == 0) {
          memoryAvailable.await();
      }
    } finally {
      globalLock.unlock();
    }
  }
  
  public void notifyMemoryAvailable() {
    globalLock.lock();
    try {
      memoryAvailable.signalAll();
    } finally {
      globalLock.unlock();
    }
  }
  
  public void synchronizeTime(int time) {
    globalLock.lock();
    try {
      scheduler.setCurrentTime(time);
      memoryManager.setCurrentTime(time);
    } finally {
      globalLock.unlock();
    }
  }
  
  public void releaseProcessResources(Process process) {
    globalLock.lock();
    try {
      memoryLock.lock();
      try {
        memoryManager.freeProcessPages(process.getPid());
        memoryAvailable.signalAll();
      } finally {
        memoryLock.unlock();
      }
      
      process.setState(ProcessState.TERMINATED);
      scheduler.onProcessComplete(process);
      
      Logger.log("Recursos del proceso " + process.getPid() + " liberados");
      
    } finally {
      globalLock.unlock();
    }
  }
  
  public void start() {
    globalLock.lock();
    try {
      running = true;
      Logger.log("SyncController iniciado");
    } finally {
      globalLock.unlock();
    }
  }
  
  public void stop() {
    globalLock.lock();
    try {
      running = false;
      processReady.signalAll();
      memoryAvailable.signalAll();
      Logger.log("SyncController detenido");
    } finally {
      globalLock.unlock();
    }
  }
  
  public void pause() {
    globalLock.lock();
    try {
      paused = true;
      Logger.log("Simulacion pausada");
    } finally {
      globalLock.unlock();
    }
  }
  
  public void resume() {
    globalLock.lock();
    try {
      paused = false;
      processReady.signalAll();
      Logger.log("Simulacion reanudada");
    } finally {
      globalLock.unlock();
    }
  }
  
  public boolean isRunning() {
    return running;
  }
  
  public boolean isPaused() {
    return paused;
  }
  
  public Scheduler getScheduler() {
    return scheduler;
  }
  
  public MemoryManager getMemoryManager() {
    return memoryManager;
  }
}