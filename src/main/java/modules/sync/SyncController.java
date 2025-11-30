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
  
  public boolean prepareProcessForExecution(Process process) {
    // Verificar estado inicial (fuera del lock para no bloquear)
    if (process.getState() != ProcessState.READY) {
      return false;
    }
    
    // Cargar páginas necesarias en memoria
    boolean hasMemory = ensureProcessMemory(process);
    
    if (!hasMemory) {
      // No hay suficiente memoria, bloquear proceso
      blockProcessForMemory(process);
      return false;
    }
    
    // Cambiar estado a RUNNING
    synchronized (coordinationMonitor) {
      if (process.getState() == ProcessState.READY) {
        // Registrar primera ejecución (tiempo de respuesta)
        if (process.getResponseTime() == -1) {
          int currentTime = scheduler.getCurrentTime();
          process.markFirstExecution(currentTime);
        }
        
        transitionState(process, ProcessState.RUNNING);
        return true;
      }
    }
    
    return false;
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
  
  private boolean ensureProcessMemory(Process process) {
    int requiredPages = process.getRequiredPages();
    
    // Verificar y cargar cada página necesaria
    for (int page = 0; page < requiredPages; page++) {
      if (!memoryManager.isPageLoaded(process.getPid(), page)) {
        // Intentar cargar la página
        boolean loaded = memoryManager.loadPage(process, page);
        
        if (!loaded) {
          // No hay memoria disponible
          return false;
        }
      }
    }
    
    // Todas las páginas están cargadas
    return true;
  }
  
  public boolean hasAllRequiredPages(Process process) {
    int requiredPages = process.getRequiredPages();
    boolean allLoaded = true;
    
    // Verificar cada página
    for (int page = 0; page < requiredPages; page++) {
      if (!memoryManager.isPageLoaded(process.getPid(), page)) {
        // Intentar cargar la página faltante
        boolean loaded = memoryManager.loadPage(process, page);
        
        if (!loaded) {
          allLoaded = false;
        }
      }
    }
    
    // Si estaba bloqueado por memoria y ahora tiene todas sus páginas, reactivarlo
    if (allLoaded && process.getState() == ProcessState.BLOCKED_MEMORY) {
      notifyProcessReady(process, "páginas cargadas en memoria");
    }
    
    return allLoaded;
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
  
  private boolean hasReadyProcesses() {
    return scheduler.hasReadyProcesses();
  }
  
  public synchronized boolean isRunning() {
    return running;
  }

  public Scheduler getScheduler() { return scheduler; }
  public MemoryManager getMemoryManager() { return memoryManager; }
  public Object getCoordinationMonitor() { return coordinationMonitor; }
}