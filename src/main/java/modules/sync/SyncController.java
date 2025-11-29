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
  private final Object coordinationMonitor = new Object();
  private volatile boolean running;
  
  public SyncController(Scheduler scheduler, MemoryManager memoryManager) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.running = false;
  }
  
  // Coordina con scheduler y memoria
  public boolean prepareProcessForExecution(Process process) {
      int t = scheduler.getCurrentTime();
      
      synchronized (coordinationMonitor) {
          if (process.getState() != ProcessState.READY) {
              return false;
          }
      }

      Burst currentBurst = process.getCurrentBurst();
      int requiredPages = calculatePagesForBurst(process, currentBurst);
      boolean pagesLoaded = checkAndLoadPages(process, requiredPages);
      
      if (!pagesLoaded) {
          blockProcessForMemory(process);
          return false;
      }
      
      synchronized (coordinationMonitor) {
        if (process.getState() == ProcessState.READY) {
          if (process.getResponseTime() == -1) {
              process.markFirstExecution(t);
          }
          transitionState(process, ProcessState.RUNNING);
          return true;
        }
      }
      
      return false; 
  }
  
  private boolean checkAndLoadPages(Process process, int requiredPages) {
    int alreadyLoadedCount = 0;
    
    // Contar páginas ya cargadas (usa locks internos de MemoryManager)
    for (int page = 0; page < requiredPages; page++) {
        if (memoryManager.isPageLoaded(process.getPid(), page)) {
            alreadyLoadedCount++;
        }
    }
    
    // Si todas están cargadas, retornar éxito
    if (alreadyLoadedCount >= requiredPages) {
        return true;
    }
    
    // Cargar las páginas faltantes
    int pagesToLoad = requiredPages - alreadyLoadedCount;
    
    int newlyLoadedCount = 0;
    
    for (int page = 0; page < requiredPages; page++) {
        // Solo cargar las que NO están ya en memoria
        if (!memoryManager.isPageLoaded(process.getPid(), page)) {
            boolean loaded = memoryManager.loadPage(process, page);
            
            if (!loaded) {
                return false;
            }
            
            newlyLoadedCount++;
        }
    }
    
    return (alreadyLoadedCount + newlyLoadedCount) >= requiredPages;
  }
  
  // Bloquea proceso por falta de memoria
  public void blockProcessForMemory(Process process) {
    synchronized (coordinationMonitor) {
      if (process.getState() != ProcessState.TERMINATED) {
        transitionState(process, ProcessState.BLOCKED_MEMORY);
        Logger.procLog("[SYNC] Proceso " + process.getPid() + " bloqueado por memoria");
        scheduler.forceContextSwitch();
      }
    }
  }
  
  // Calcula páginas necesarias para la ráfaga
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
    synchronized (coordinationMonitor) {
      ProcessState previousState = process.getState();

      if (process.getState() == ProcessState.TERMINATED) {
        Logger.procLog("[SYNC] Proceso ya terminado");
        return;
      }
      
      transitionState(process, ProcessState.READY);
      
      // Agregar a cola del scheduler (usa su propia sincronización interna)
      scheduler.addProcess(process);
      
      // Notificación explícita cuando pasa de BLOQUEADO -> READY
      if (previousState.isBlocked() && process.getState() == ProcessState.READY) {
        Logger.procLog(String.format(
          "[SYNC] Proceso %s pasa de BLOQUEADO (%s) a READY (razón: %s)",
          process.getPid(), previousState, reason
        ));
      }
      
      // Despertar threads esperando
      coordinationMonitor.notifyAll();
    }
  }
  
  // Espera a que haya procesos listos
  public void waitForReadyProcess() throws InterruptedException {
    synchronized (coordinationMonitor) {
      while (running && !hasReadyProcesses()) {
        coordinationMonitor.wait();
      }
    }
  }
  
  // Verifica si hay procesos listos (delegado al scheduler)
  private boolean hasReadyProcesses() {
    return scheduler.hasReadyProcesses();
  }
  
  // Libera recursos de un proceso terminado
  public void releaseProcessResources(Process process) {    
    synchronized (coordinationMonitor) {
      // Cambiar estado
      transitionState(process, ProcessState.TERMINATED);
      
      // Liberar memoria (usa sincronización interna de MemoryManager)
      memoryManager.freeProcessPages(process.getPid());
      
      // Notificar al scheduler para métricas
      scheduler.onProcessComplete(process);
      
      // Despertar threads que pueden estar esperando recursos
      coordinationMonitor.notifyAll();
    }
  }

  public boolean hasRequiredPages(Process process) {
    int requiredPages = process.getRequiredPages();
    boolean allPagesLoaded = true;
    
    for (int page = 0; page < requiredPages; page++) {
        if (!memoryManager.isPageLoaded(process.getPid(), page)) {
            // Intentar cargar la página (MemoryManager sincroniza internamente)
            boolean loaded = memoryManager.loadPage(process, page);
            if (!loaded) {
                allPagesLoaded = false;
            }
        }
    }
    
    // Si el proceso estaba bloqueado por memoria y ahora tiene todas sus páginas, reactivarlo
    if (allPagesLoaded && process.getState() == ProcessState.BLOCKED_MEMORY) {
        notifyProcessReady(process, "páginas cargadas");
    }
    
    return allPagesLoaded;
  }

  public boolean canProcessExecute(Process process) {
    synchronized (coordinationMonitor) {
      // Verificar que el proceso esté listo o ya ejecutando
      if (process.getState() != ProcessState.READY && 
          process.getState() != ProcessState.RUNNING) {
        return false;
      }
    }
    // Comprobación de páginas fuera del lock principal para evitar secciones críticas largas
    return hasRequiredPages(process);
  }
  
  public void synchronizeTime(int time) {
    // Un solo hilo (SimulationEngine) avanza el tiempo, por lo que
    // no necesitamos monitores adicionales aquí.
    scheduler.setCurrentTime(time);
    memoryManager.setCurrentTime(time);
  }
  
  public synchronized void start() {
    running = true;
  }
  
  public synchronized void stop() {
    running = false;
    synchronized (coordinationMonitor) {
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
  
  public Object getCoordinationMonitor() {
    return coordinationMonitor;
  }
}