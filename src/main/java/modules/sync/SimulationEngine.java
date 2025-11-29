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
  
  private SimulationStateListener stateListener;

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

  // ✅ NUEVO: Método para registrar el listener
  public void setStateListener(SimulationStateListener listener) {
    this.stateListener = listener;
  }
  
  // ✅ NUEVO: Notificar cambios a la UI
  private void notifyUIUpdate() {
    if (stateListener != null) {
      List<Process> readyQueue = scheduler.getReadyQueueSnapshot();
      
      List<Process> blockedIO;
      List<Process> blockedMemory;
      
      synchronized(engineMonitor) {
        blockedIO = allProcesses.stream()
          .filter(p -> p.getState() == ProcessState.BLOCKED_IO)
          .toList();
        
        blockedMemory = allProcesses.stream()
          .filter(p -> p.getState() == ProcessState.BLOCKED_MEMORY)
          .toList();
      }
      
      stateListener.onReadyQueueChanged(readyQueue);
      stateListener.onBlockedIOChanged(blockedIO);
      stateListener.onBlockedMemoryChanged(blockedMemory);
      stateListener.onTimeChanged(currentTime);
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
        // SINCRONIZAR TIEMPO PRIMERO (antes de hacer nada)
        syncController.synchronizeTime(getCurrentTime());
        
        notifyTimeAdvance();

        // Coordinar con el planificador (ANTES de incrementar tiempo de espera)
        coordinateScheduler();

        sleep(config.getTimeUnit());

        // Incrementar tiempo de espera SOLO para procesos que permanecen en READY
        // después de que el scheduler haya seleccionado procesos
        synchronized(engineMonitor) {
            for (Process p : allProcesses) {
                if (p.getState() == ProcessState.READY) {
                    p.incrementWaitingTime();
                }
            }
        }

        // Notificar procesos bloqueados en I/O
        synchronized(engineMonitor) {
            for (ProcessThread thread : processThreads) {
                Process p = thread.getProcess();
                if (p.getState() == ProcessState.BLOCKED_IO) {
                    synchronized(p) {
                        p.notifyAll();
                    }
                }
            }
        }

        synchronized(engineMonitor) {
            for (ProcessThread thread : processThreads) {
                Process p = thread.getProcess();
                if (p.getState() == ProcessState.BLOCKED_MEMORY) {
                    thread.wakeUp();
                }
            }
        }
      }

      if (getCurrentTime() % 5 == 0) {
        printSystemState();
      }
      
      // Sincronizar tiempo global
      syncController.synchronizeTime(getCurrentTime());
      
      System.out.println("[Engine] Notificando UI en tiempo: " + currentTime);
      notifyUIUpdate();
      
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
    
    if (currentRunning != null && currentRunning.getState() == ProcessState.RUNNING) {
      if (!syncController.hasRequiredPages(currentRunning)) {

        Logger.log("[ENGINE] " + currentRunning.getPid() + " perdió páginas durante ejecución");
        currentRunning.setState(ProcessState.READY);
        scheduler.addProcess(currentRunning);
        scheduler.setCurrentProcess(null);
        currentRunning = null;
      }
    }

    if (currentRunning != null && currentRunning.getState() == ProcessState.TERMINATED) {
      scheduler.setCurrentProcess(null);
      currentRunning = null;
    }

    if (currentRunning != null && currentRunning.getState() == ProcessState.BLOCKED_IO) {
      scheduler.setCurrentProcess(null);
      currentRunning = null;
    }

    if (currentRunning != null && currentRunning.getState() == ProcessState.BLOCKED_MEMORY) {
      scheduler.setCurrentProcess(null);
      currentRunning = null;
    }
    // Cuando el scheduler selecciona un proceso diferente al actual,
    // se realiza un cambio de contexto lógico:
    if (currentRunning != nextProcess) {
        // Cambiar proceso actual de RUNNING a READY
        if (currentRunning != null && currentRunning.getState() == ProcessState.RUNNING) {
            synchronized(syncController.getCoordinationMonitor()) {
                currentRunning.setState(ProcessState.READY);
            }
            // El thread de currentRunning se bloqueará en su próximo wait()
        }
        
        // Preparar nuevo proceso (verificar memoria)
        boolean canExecute = syncController.prepareProcessForExecution(nextProcess);
        
        if (canExecute) {
            // Confirmar selección y registrar cambio de contexto
            scheduler.confirmProcessSelection(nextProcess);  // Incrementa contextSwitches
            // Despertar thread del nuevo proceso
            wakeUpThread(nextProcess);  // El thread sale de wait() y continúa
            scheduler.recordCPUTime(1);
        } else {
            // Proceso no puede ejecutarse (falta memoria), queda en READY
            scheduler.recordIdleTime(1);
            scheduler.addProcess(nextProcess);
        }
    } 
    // El mismo proceso continúa
    else if (nextProcess != null) {
        boolean canExecute = syncController.canProcessExecute(nextProcess);
        
        if (canExecute) {
            wakeUpThread(nextProcess);
            scheduler.recordCPUTime(1);
        } else {
            scheduler.setCurrentProcess(null);
            scheduler.recordIdleTime(1);
            scheduler.addProcess(nextProcess);
        }
    }
  }
  
  private void wakeUpThread(Process process) {
    synchronized(engineMonitor) {
      for (ProcessThread thread : processThreads) {
        if (thread.getProcess() == process) {
          thread.wakeUp();  // Internamente hace notifyAll() en threadMonitor
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
      long blockedMem = allProcesses.stream()
        .filter(p -> p.getState() == ProcessState.BLOCKED_MEMORY)
        .count();
      long blockedIO = allProcesses.stream()
        .filter(p -> p.getState() == ProcessState.BLOCKED_IO)
        .count();
      
      Logger.log("BLOQUEADOS MEMORIA: " + blockedMem + " procesos");
      allProcesses.stream()
        .filter(p -> p.getState() == ProcessState.BLOCKED_MEMORY)
        .forEach(p -> Logger.log("     [MEM] " + p.getPid()));
      
      Logger.log("BLOQUEADOS I/O: " + blockedIO + " procesos");
      allProcesses.stream()
        .filter(p -> p.getState() == ProcessState.BLOCKED_IO)
        .forEach(p -> Logger.log("     [I/O] " + p.getPid()));
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