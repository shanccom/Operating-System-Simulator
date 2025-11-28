package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;

public class SyncController {
  
  private final Scheduler scheduler;
  private final MemoryManager memoryManager;
  private final Object schedulerMonitor = new Object();
  private final Object memoryMonitor = new Object();
  private final Object coordinationMonitor = new Object();
  private volatile boolean running;
  
  public SyncController(Scheduler scheduler, MemoryManager memoryManager) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.running = false;
  }
  
  // Coordina con scheduler y memoria
  public boolean prepareProcessForExecution(Process process) {
    Logger.log("[SYNC] Preparando proceso " + process.getPid());
    
    // Verificar estado con coordinationMonitor
    synchronized(coordinationMonitor) {
      Logger.log("[SYNC] >>> Monitor de COORDINACIÓN adquirido por " + process.getPid());
      
      if (process.getState() != ProcessState.READY) {
        Logger.log("[SYNC] Proceso no está READY");
        Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
        return false;
      }
      
      Burst currentBurst = process.getCurrentBurst();
      if (currentBurst == null || !currentBurst.isCPU()) {
        Logger.log("[SYNC] Sin ráfaga CPU válida");
        Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
        return false;
      }
      
      Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
    }
    
    // Cargar páginas con memoryMonitor
    synchronized(memoryMonitor) {
      Logger.log("[SYNC] >>> Monitor de MEMORIA adquirido por " + process.getPid());
      
      Burst currentBurst = process.getCurrentBurst();
      int requiredPages = calculatePagesForBurst(process, currentBurst);
      
      boolean pagesLoaded = checkAndLoadPages(process, requiredPages);
      
      Logger.log("[SYNC] <<< Monitor de MEMORIA liberado por " + process.getPid());
      
      if (!pagesLoaded) {
        // Bloquear proceso por falta de memoria
        blockProcessForMemory(process);
        return false;
      }
    }
    
    // Cambiar estado a RUNNING
    synchronized(coordinationMonitor) {
      Logger.log("[SYNC] >>> Monitor de COORDINACIÓN adquirido por " + process.getPid());
      
      if (process.getState() == ProcessState.READY) {
        transitionState(process, ProcessState.RUNNING);
        Logger.log("[SYNC] Proceso " + process.getPid() + " ahora RUNNING");
        Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
        return true;
      }
      
      Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
      return false;
    }
  }
  
  private boolean checkAndLoadPages(Process process, int requiredPages) {
    int alreadyLoadedCount = 0;
    
    // Contar páginas ya cargadas
    for (int page = 0; page < requiredPages; page++) {
        if (memoryManager.isPageLoaded(process.getPid(), page)) {
            alreadyLoadedCount++;
        }
    }
    
    // Si todas están cargadas, retornar éxito
    if (alreadyLoadedCount >= requiredPages) {
        Logger.log("[SYNC] Todas las páginas ya cargadas (" + alreadyLoadedCount + "/" + requiredPages + ")");
        return true;
    }
    
    // Cargar las páginas faltantes
    int pagesToLoad = requiredPages - alreadyLoadedCount;
    Logger.log("[SYNC] Cargando " + pagesToLoad + " páginas nuevas para " + process.getPid());
    
    int newlyLoadedCount = 0;
    
    for (int page = 0; page < requiredPages; page++) {
        // Solo cargar las que NO están ya en memoria
        if (!memoryManager.isPageLoaded(process.getPid(), page)) {
            boolean loaded = memoryManager.loadPage(process, page);
            
            if (!loaded) {
                Logger.log("[SYNC] No hay marcos disponibles");
                return false;
            }
            
            newlyLoadedCount++;
            // Mostrar progreso de las páginas NUEVAS, no del total
            Logger.log("[SYNC] Página " + page + " cargada (" + newlyLoadedCount + "/" + pagesToLoad + ")");
        }
    }
    
    return (alreadyLoadedCount + newlyLoadedCount) >= requiredPages;
}
  
  // Bloquea proceso por falta de memoria
  private void blockProcessForMemory(Process process) {
    synchronized(coordinationMonitor) {
      Logger.log("[SYNC] >>> Monitor de COORDINACIÓN adquirido para bloquear");
      
      if (process.getState() != ProcessState.TERMINATED) {
        transitionState(process, ProcessState.BLOCKED_MEMORY);
        Logger.log("[SYNC] Proceso " + process.getPid() + " bloqueado por memoria");
      }
      
      Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
    }
  }
  
  //Calcula páginas necesarias para la ráfaga
  private int calculatePagesForBurst(Process process, Burst burst) {
    /** 
    int totalPages = process.getRequiredPages();
    int basePages = Math.max(1, (int) Math.ceil(totalPages * 0.4));
    
    if (burst.isCPU()) {
      return Math.min(basePages, totalPages);
    } else {
      return Math.min(basePages + 1, totalPages);
    }
    */
    return process.getRequiredPages();
  }
  
  private void transitionState(Process process, ProcessState newState) {
    ProcessState oldState = process.getState();
    if (oldState != newState) {
      Logger.logStateChange(process.getPid(), oldState, newState, scheduler.getCurrentTime());
      process.setState(newState);
    }
  }
  
  // Notifica que un proceso está listo
  public void notifyProcessReady(Process process, String reason) {
    synchronized(coordinationMonitor) {
      Logger.log("[SYNC] >>> Monitor de COORDINACIÓN adquirido");
      Logger.log("[SYNC] Proceso " + process.getPid() + " listo (" + reason + ")");
      
      if (process.getState() == ProcessState.TERMINATED) {
        Logger.log("[SYNC] Proceso ya terminado");
        Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
        return;
      }
      
      transitionState(process, ProcessState.READY);
      
      // Agregar a cola del scheduler
      synchronized(schedulerMonitor) {
        Logger.log("[SYNC] >>> Monitor de SCHEDULER adquirido");
        scheduler.addProcess(process);
        Logger.log("[SYNC] Proceso agregado a cola de listos");
        Logger.log("[SYNC] <<< Monitor de SCHEDULER liberado");
      }
      
      // Despertar threads esperando
      coordinationMonitor.notifyAll();
      Logger.log("[SYNC] notifyAll() ejecutado en coordinationMonitor");
      Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
    }
  }
  
  // Espera a que haya procesos listos
  public void waitForReadyProcess() throws InterruptedException {
    synchronized(coordinationMonitor) {
      Logger.log("[SYNC] >>> Monitor de COORDINACIÓN adquirido para esperar");
      
      while (running && !hasReadyProcesses()) {
        Logger.log("[SYNC] No hay procesos listos, esperando en coordinationMonitor.wait()...");
        coordinationMonitor.wait(); // Libera el monitor y espera
        Logger.log("[SYNC] Despertado de coordinationMonitor.wait()");
      }
      
      Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
    }
  }
  
  //  Verifica si hay procesos listos
  
  private boolean hasReadyProcesses() {
    synchronized(schedulerMonitor) {
      return scheduler.hasReadyProcesses();
    }
  }
  
  // Libera recursos de un proceso terminado
  public void releaseProcessResources(Process process) {
    Logger.log("[SYNC] Liberando recursos de proceso " + process.getPid());
    
    synchronized(coordinationMonitor) {
      Logger.log("[SYNC] >>> Monitor de COORDINACIÓN adquirido");
      
      // Cambiar estado
      transitionState(process, ProcessState.TERMINATED);
      
      // Liberar memoria
      synchronized(memoryMonitor) {
        Logger.log("[SYNC] >>> Monitor de MEMORIA adquirido");
        memoryManager.freeProcessPages(process.getPid());
        Logger.log("[SYNC] Páginas liberadas");
        Logger.log("[SYNC] <<< Monitor de MEMORIA liberado");
      }
      
      // Notificar al scheduler
      synchronized(schedulerMonitor) {
        Logger.log("[SYNC] >>> Monitor de SCHEDULER adquirido");
        scheduler.onProcessComplete(process);
        Logger.log("[SYNC] <<< Monitor de SCHEDULER liberado");
      }
      
      // Despertar threads que pueden estar esperando recursos
      coordinationMonitor.notifyAll();
      Logger.log("[SYNC] notifyAll() ejecutado - recursos liberados");
      Logger.log("[SYNC] <<< Monitor de COORDINACIÓN liberado");
    }
  }
  
  public synchronized void synchronizeTime(int time) {
    synchronized(schedulerMonitor) {
      scheduler.setCurrentTime(time);
    }
    
    synchronized(memoryMonitor) {
      memoryManager.setCurrentTime(time);
    }
  }
  
  public synchronized void start() {
    running = true;
  }
  
  public synchronized void stop() {
    running = false;
    synchronized(coordinationMonitor) {
      coordinationMonitor.notifyAll();
    }
  }
  
  public synchronized boolean isRunning() {
    return running;
  }

  public Scheduler getScheduler() {
    return scheduler;
  }
  
  public MemoryManager getMemoryManager() {
    return memoryManager;
  }
  
  public Object getSchedulerMonitor() {
    return schedulerMonitor;
  }
  
  public Object getMemoryMonitor() {
    return memoryMonitor;
  }
  
  public Object getCoordinationMonitor() {
    return coordinationMonitor;
  }
}