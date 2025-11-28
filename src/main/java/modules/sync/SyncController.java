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
      int t = scheduler.getCurrentTime();
      
      // VERIFICAR que el proceso esté listo
      synchronized(coordinationMonitor) {
          if (process.getState() != ProcessState.READY) {
              return false;
          }
      }

      // CARGAR PÁGINAS
      boolean pagesLoaded = false;
      synchronized(memoryMonitor) {
          Burst currentBurst = process.getCurrentBurst();
          int requiredPages = calculatePagesForBurst(process, currentBurst);
          pagesLoaded = checkAndLoadPages(process, requiredPages);
      }
      
      if (!pagesLoaded) {
          blockProcessForMemory(process);
          return false;
      }
      
      // SOLO CUANDO LAS PÁGINAS ESTÉN CARGADAS cambiar estado
      synchronized(coordinationMonitor) {
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
    
    // Contar páginas ya cargadas
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
  private void blockProcessForMemory(Process process) {
    synchronized(coordinationMonitor) {
      
      if (process.getState() != ProcessState.TERMINATED) {
        transitionState(process, ProcessState.BLOCKED_MEMORY);
        Logger.log("[SYNC] Proceso " + process.getPid() + " bloqueado por memoria");
        scheduler.forceContextSwitch();
      }
      
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
      
      if (process.getState() == ProcessState.TERMINATED) {
        Logger.log("[SYNC] Proceso ya terminado");
        return;
      }
      
      transitionState(process, ProcessState.READY);
      
      // Agregar a cola del scheduler
      synchronized(schedulerMonitor) {
        scheduler.addProcess(process);
      }
      
      // Despertar threads esperando
      coordinationMonitor.notifyAll();
    }
  }
  
  // Espera a que haya procesos listos
  public void waitForReadyProcess() throws InterruptedException {
    synchronized(coordinationMonitor) {
      
      while (running && !hasReadyProcesses()) {
        coordinationMonitor.wait();
      }
      
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
    synchronized(coordinationMonitor) {
      
      // Cambiar estado
      transitionState(process, ProcessState.TERMINATED);
      
      // Liberar memoria
      synchronized(memoryMonitor) {
        memoryManager.freeProcessPages(process.getPid());
      }
      
      // Notificar al scheduler
      synchronized(schedulerMonitor) {
        scheduler.onProcessComplete(process);
      }
      
      // Despertar threads que pueden estar esperando recursos
      coordinationMonitor.notifyAll();
    }
  }

  public boolean hasRequiredPages(Process process) {
    synchronized(memoryMonitor) {
        int requiredPages = process.getRequiredPages();
        boolean allPagesLoaded = true;
        
        for (int page = 0; page < requiredPages; page++) {
            if (!memoryManager.isPageLoaded(process.getPid(), page)) {
                // INTENTAR CARGAR LA PÁGINA
                boolean loaded = memoryManager.loadPage(process, page);
                if (!loaded) {
                    allPagesLoaded = false;
                }
            }
        }
        
        // SI EL PROCESO ESTABA BLOQUEADO Y AHORA TIENE PÁGINAS, REACTIVARLO
        if (allPagesLoaded && process.getState() == ProcessState.BLOCKED_MEMORY) {
            notifyProcessReady(process, "páginas cargadas");
        }
        
        return allPagesLoaded;
    }
  }

  public boolean canProcessExecute(Process process) {
    synchronized(coordinationMonitor) {
      // Verificar que el proceso esté listo y tenga sus páginas
      if (process.getState() != ProcessState.READY && 
        process.getState() != ProcessState.RUNNING) {
        return false;
      }
      return hasRequiredPages(process);
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