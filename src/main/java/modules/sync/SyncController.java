package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;
import model.Config;

public class SyncController {
  
  private final Scheduler scheduler;
  private final MemoryManager memoryManager;
  private final Object coordinationMonitor = new Object();
  private volatile boolean running;
  private final Config config;

  public SyncController(Scheduler scheduler, MemoryManager memoryManager, Config config) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.running = false;
    this.config = config;
  }
  
  public boolean prepareProcessForExecution(Process process) {
      // ✅ NUEVO: Verificar si está esperando que termine el page fault
      if (process.isWaitingForPageFault()) {
          int currentTime = scheduler.getCurrentTime();
          int endTime = process.getPageFaultEndTime();
          
          if (currentTime < endTime) {      
              return false;
          } else {
              // Ya terminó el page fault penalty
              Logger.memLog(String.format("[T=%d] [PAGE FAULT] ✓ %s completó page fault handling", 
                  currentTime, process.getPid()));
              process.clearPageFault();
              // Continuar con la verificación normal
          }
      }
      
      // Verificar si tiene todas las páginas necesarias
      if (hasAllRequiredPages(process)) {
          process.setState(ProcessState.RUNNING);
          return true;
      }
      
      // ✅ NUEVO: Necesita cargar páginas con overhead
      int currentTime = scheduler.getCurrentTime();
      
      Logger.memLog(String.format("[T=%d] [PAGE FAULT] %s necesita cargar páginas", 
          currentTime, process.getPid()));
      
      // Intentar cargar páginas
      boolean hadPageFaults = loadRequiredPages(process);
      
      if (hadPageFaults) {
          // ✅ Páginas cargadas exitosamente, aplicar penalty
          int pageFaultPenalty = config.getPageFaultPenalty();
          int endTime = currentTime + pageFaultPenalty;
          
          process.setState(ProcessState.BLOCKED_MEMORY);
          process.setPageFaultEndTime(endTime);
          
          Logger.memLog(String.format("[T=%d] [PAGE FAULT] %s bloqueado hasta t=%d (penalty: %d ciclos)", 
              currentTime, process.getPid(), endTime, pageFaultPenalty));
          
          return false;  // No puede ejecutar aún
      } else {
        process.setState(ProcessState.RUNNING);
        return true;
      }
  }
  
  public boolean canProcessExecute(Process process) {
    ProcessState state = process.getState();
    
    // Solo verificar si está en READY o RUNNING
    if (state != ProcessState.READY && state != ProcessState.RUNNING) {
      return false;
    }
    
    // Verificar que tenga todas sus páginas
    return hasAllRequiredPages(process);
  }
  
  public boolean hasAllRequiredPages(Process process) {
    int requiredPages = process.getRequiredPages();
    String pid = process.getPid();
    
    for (int page = 0; page < requiredPages; page++) {
        if (!memoryManager.isPageLoaded(pid, page)) {
            return false;  // Falta al menos una página
        }
    }
    
    // Todas las páginas están cargadas
    return true;
}
  
  public void blockProcessForMemory(Process process) {
    synchronized (coordinationMonitor) {
      if (process.getState() != ProcessState.TERMINATED) {
        transitionState(process, ProcessState.BLOCKED_MEMORY);
        
        int currentTime = scheduler.getCurrentTime();
        Logger.procLog(String.format("[T=%d] [%s] → BLOCKED_MEMORY (memoria insuficiente)", 
          currentTime, process.getPid()));
        
        // Forzar cambio de contexto en el scheduler
        scheduler.forceContextSwitch();
      }
    }
  }
  
  private void transitionState(Process process, ProcessState newState) {
    ProcessState oldState = process.getState();
    
    if (oldState != newState) {
      int currentTime = scheduler.getCurrentTime();
      Logger.logStateChange(process.getPid(), oldState, newState, currentTime);
      process.setState(newState);
    }
  }
  
  public void notifyProcessReady(Process process, String reason) {
    synchronized (coordinationMonitor) {
      ProcessState previousState = process.getState();
      
      // Verificar que no esté terminado
      if (previousState == ProcessState.TERMINATED) {
        return;
      }
      
      // Cambiar estado a READY
      transitionState(process, ProcessState.READY);
      
      // Agregar al scheduler (usa su propia sincronización interna)
      scheduler.addProcess(process);
      
      // Log especial cuando viene de un estado bloqueado
      if (previousState.isBlocked()) {
        int currentTime = scheduler.getCurrentTime();
        Logger.procLog(String.format(
          "[T=%d] [%s] %s → READY (%s)",
          currentTime, process.getPid(), previousState, reason
        ));
      }
      
      // Despertar threads que puedan estar esperando
      coordinationMonitor.notifyAll();
    }
  }
  
  public void releaseProcessResources(Process process) {
    synchronized (coordinationMonitor) {
      // Cambiar estado a TERMINATED
      transitionState(process, ProcessState.TERMINATED);
      
      int currentTime = scheduler.getCurrentTime();
      Logger.procLog(String.format("[T=%d] [%s] → TERMINATED", 
        currentTime, process.getPid()));
      
      // Liberar memoria (MemoryManager usa sincronización interna)
      memoryManager.freeProcessPages(process.getPid());
      
      // Notificar al scheduler para actualizar métricas
      scheduler.onProcessComplete(process);
      
      // Despertar threads que puedan estar esperando recursos
      coordinationMonitor.notifyAll();
    }
  }
  
  public void synchronizeTime(int time) {
    scheduler.setCurrentTime(time);
    memoryManager.setCurrentTime(time);
  }
  

  public synchronized void start() {
    running = true;
    Logger.syncLog("[SYNC] Controlador iniciado");
  }
  
  public synchronized void stop() {
    running = false;
    synchronized (coordinationMonitor) {
      coordinationMonitor.notifyAll();
    }
    Logger.syncLog("[SYNC] Controlador detenido");
  }
  
  public void waitForReadyProcess() throws InterruptedException {
    synchronized (coordinationMonitor) {
      while (running && !hasReadyProcesses()) {
        coordinationMonitor.wait();
      }
    }
  }
  
  public void triggerReschedule() {
    // Despertar al motor de simulación para que seleccione el siguiente proceso
    synchronized(getCoordinationMonitor()) {
        getCoordinationMonitor().notifyAll();
    }
  }

  private boolean hasReadyProcesses() {
    return scheduler.hasReadyProcesses();
  }
  
  public synchronized boolean isRunning() {
    return running;
  }

  private boolean loadRequiredPages(Process process) {
    int numPages = process.getRequiredPages();
    String pid = process.getPid();
    boolean hadPageFault = false;

    // Intentar cargar cada página que no esté ya en memoria
    for (int pageNum = 0; pageNum < numPages; pageNum++) {
        // Verificar si la página ya está cargada
        if (!memoryManager.isPageLoaded(pid, pageNum)) {
            hadPageFault = true;
            // Intentar cargar la página
            boolean loaded = memoryManager.loadPage(process, pageNum);
            
            if (!loaded) {
                // No se pudo cargar esta página
                Logger.memLog(String.format("[PAGE FAULT] No se pudo cargar página %d de %s", 
                    pageNum, pid));
                return false;
            }
        }
    }
    
    // Todas las páginas fueron cargadas exitosamente
    return hadPageFault;
}

  public Scheduler getScheduler() { return scheduler; }
  public MemoryManager getMemoryManager() { return memoryManager; }
  public Object getCoordinationMonitor() { return coordinationMonitor; }
}