package modules.sync;
import model.Config;
//NUEVO CAMBIO DE PARTE DE BRANCH MEM_MOD CAMBIOS DE DEFINICION DE PASO ACA TAMBIEN EN EVENTOS DE MEMORIA EN SINCRONIZACION
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
  private final Config config;

  public SyncController(Scheduler scheduler, MemoryManager memoryManager, Config config) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.running = false;
    this.config = config;
  }
  
  public boolean prepareProcessForExecution(Process process) {
    synchronized(coordinationMonitor) {
      if (process.isWaitingForPageFault()) {
        int currentTime = scheduler.getCurrentTime();
        int endTime = process.getPageFaultEndTime();
        
        if (currentTime < endTime) {      
          return false;
        } else {
          Logger.memLog(String.format("[T=%d] [PAGE FAULT] %s completó page fault handling", 
            currentTime, process.getPid()));
          process.clearPageFault();
          memoryManager.waitForVisualStep();
        }
      } 
      
      // Verificar si tiene todas las páginas necesarias
      if (hasAllRequiredPages(process)) {
        process.setState(ProcessState.RUNNING);
        return true;
      }
      
      int currentTime = scheduler.getCurrentTime();
    
      // Intentar cargar páginas
      boolean hadPageFaults = loadRequiredPages(process);
      
      if (hadPageFaults) {
        int pageFaultPenalty = config.getPageFaultPenalty();
        
        if (pageFaultPenalty > 0) {
          int endTime = currentTime + pageFaultPenalty;
          
          process.setState(ProcessState.BLOCKED_MEMORY);
          process.setPageFaultEndTime(endTime);
          
          Logger.exeLog(String.format("[T=%d] [PAGE FAULT] %s bloqueado hasta t=%d (penalty: %d ciclos)", 
            currentTime, process.getPid(), endTime, pageFaultPenalty));
          memoryManager.waitForVisualStep();
          return false;
        } else {
          Logger.exeLog(String.format("[PAGE FAULT] %s páginas cargadas (penalty=0, continúa)", 
             process.getPid()));
          memoryManager.waitForVisualStep();
          process.setState(ProcessState.RUNNING);
          return true;
        }
      } else {
        process.setState(ProcessState.RUNNING);
        return true;
      }
    }
  }
  
  public boolean canProcessExecute(Process process) {
    synchronized(coordinationMonitor) {
      ProcessState state = process.getState();
      
      if (state != ProcessState.READY && state != ProcessState.RUNNING) {
        return false;
      }
      
      return hasAllRequiredPages(process);
    }
  }
  
  public boolean hasAllRequiredPages(Process process) {
    synchronized(coordinationMonitor) {
      int requiredPages = process.getRequiredPages();
      String pid = process.getPid();
      
      for (int page = 0; page < requiredPages; page++) {
        if (!memoryManager.isPageLoaded(pid, page)) {
          return false;
        }
      }
      
      return true;
    }
  }
  
  public void blockProcessForMemory(Process process) {
    synchronized(coordinationMonitor) {
      ProcessState currentState = process.getState();
      if (currentState != ProcessState.TERMINATED) {
        process.setState(ProcessState.BLOCKED_MEMORY);
        
        int currentTime = scheduler.getCurrentTime();
        Logger.procLog(String.format("[T=%d] [%s] BLOCKED_MEMORY (memoria insuficiente)", 
          currentTime, process.getPid()));
        
        scheduler.forceContextSwitch();
        coordinationMonitor.notifyAll();
      }
    }
  }
  
  public void notifyProcessReady(Process process, String reason) {
    synchronized(coordinationMonitor) {
      ProcessState previousState = process.getState();
      
      if (previousState == ProcessState.TERMINATED) {
        return;
      }
      
      process.setState(ProcessState.READY);
      scheduler.addProcess(process);
      
      if (previousState.isBlocked()) {
        int currentTime = scheduler.getCurrentTime();
        Logger.procLog(String.format(
          "[T=%d] [%s] %s READY (%s)",
          currentTime, process.getPid(), previousState, reason
        ));
      }
      
      coordinationMonitor.notifyAll();
    }
  }
  
  public void releaseProcessResources(Process process) {
    synchronized(coordinationMonitor) {
      process.setState(ProcessState.TERMINATED);
      
      int currentTime = scheduler.getCurrentTime();
      Logger.procLog(String.format("[T=%d] [%s] TERMINATED", 
        currentTime, process.getPid()));
        
      memoryManager.freeProcessPages(process.getPid());
      
      scheduler.onProcessComplete(process);
      
      coordinationMonitor.notifyAll();
    }
  }
  
  public void synchronizeTime(int time) {
    synchronized(coordinationMonitor) {
      scheduler.setCurrentTime(time);
      memoryManager.setCurrentTime(time);
    }
  }

  public synchronized int getCurrentTime() {
    return scheduler.getCurrentTime();
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
  
  public void waitForReadyProcess() throws InterruptedException {
    synchronized (coordinationMonitor) {
      while (running && !hasReadyProcesses()) {
        coordinationMonitor.wait();
      }
    }
  }
  
  public void triggerReschedule() {
    synchronized(coordinationMonitor) {
      coordinationMonitor.notifyAll();
    }
  }

  private boolean hasReadyProcesses() {
    return scheduler.hasReadyProcesses();
  }
  
  public synchronized boolean isRunning() {
    return running;
  }

  private boolean loadRequiredPages(Process process) {
    synchronized(coordinationMonitor) {
      int numPages = process.getRequiredPages();
      String pid = process.getPid();
      boolean hadPageFault = false;

      for (int pageNum = 0; pageNum < numPages; pageNum++) { // Carga la pagina si no esta cargada
        if (!memoryManager.isPageLoaded(pid, pageNum)) {
          hadPageFault = true;
          boolean loaded = memoryManager.loadPage(process, pageNum);
          if (!loaded) {
            Logger.memLog(String.format("[PAGE FAULT] No se pudo cargar página %d de %s", 
              pageNum, pid));
            return false;
          }
        }
      }
      
      return hadPageFault;
    }
  }


  public Scheduler getScheduler() { return scheduler; }
  public MemoryManager getMemoryManager() { return memoryManager; }
  public Object getCoordinationMonitor() { return coordinationMonitor; }
}