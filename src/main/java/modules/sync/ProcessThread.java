package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger;
import model.Config;

public class ProcessThread extends Thread {

  private final Process process;
  private final SyncController syncController;
  private final IOManager ioManager;
  private final Config config;
  
  // Monitor local para sincronización interna del thread
  private final Object threadMonitor = new Object();
  private volatile boolean running;

  public ProcessThread(Process process, SyncController syncController, 
                       IOManager ioManager, Config config) {
    super("Thread-" + process.getPid());
    this.process = process;
    this.syncController = syncController;
    this.ioManager = ioManager;
    this.config = config;
    this.running = true;
  }

  @Override 
  public void run() {
    try {
      // 1. Esperar hasta el tiempo de llegada
      waitForArrival();
      
      // 2. Notificar llegada al sistema
      arriveAtSystem();
      
      // 3. Loop principal: ejecutar ráfagas hasta completar
      mainExecutionLoop();
      
    } catch (InterruptedException e) {
      Logger.warning("[THREAD-" + process.getPid() + "] Interrumpido");
      Thread.currentThread().interrupt();
    } finally {
      // Asegurar que el proceso termine correctamente
      if (process.getState() != ProcessState.TERMINATED) {
        terminateProcess();
      }
    }
  }

  private void waitForArrival() throws InterruptedException {
    int arrivalTime = process.getArrivalTime();
    
    synchronized(threadMonitor) {
      while(running) {
        // Leer tiempo fuera del monitor para evitar deadlocks
        int currentTime = syncController.getScheduler().getCurrentTime();
        
        if (currentTime >= arrivalTime) {
          break; // Ya llegó el momento
        }
        
        // Esperar un poco y volver a verificar
        threadMonitor.wait(50);
      }
    }
    
    Logger.procLog(String.format("[T=%d] [%s] Proceso llegó al sistema", 
      arrivalTime, process.getPid()));
  }

  private void arriveAtSystem() {
    // Cambiar estado localmente (sin locks externos)
    if (process.getState() != ProcessState.TERMINATED) {
      process.setState(ProcessState.READY);
    }
    
    // Notificar al controlador (usa su propio lock)
    // IMPORTANTE: Esto se hace FUERA de threadMonitor para evitar deadlocks
    if (process.getState() == ProcessState.READY) {
      syncController.notifyProcessReady(process, "llegada al sistema");
    }
  }

  private void mainExecutionLoop() throws InterruptedException {
    while (running && !process.isCompleted()) {
      // Esperar a ser seleccionado por el scheduler
      waitForRunningState();
      
      // Verificar si debemos terminar
      ProcessState currentState = process.getState();
      if (!running || currentState == ProcessState.TERMINATED) {
        break;
      }
      
      // Obtener ráfaga actual
      Burst currentBurst = process.getCurrentBurst();
      
      if (currentBurst == null) {
        // No hay más ráfagas, terminar
        break;
      }
      
      // Ejecutar la ráfaga según su tipo
      if (currentBurst.isCPU()) {
        executeCPUBurst(currentBurst);
      } else {
        executeIOBurst(currentBurst);
      }
      
      // Avanzar a la siguiente ráfaga si la actual terminó
      handleBurstCompletion(currentBurst);
    }
    
    // Terminar el proceso
    terminateProcess();
  }

  private void waitForRunningState() throws InterruptedException {
    synchronized(threadMonitor) {
      while(running) {
        ProcessState state = process.getState();
        
        // Salir si ya está en RUNNING o TERMINATED
        if (state == ProcessState.RUNNING || state == ProcessState.TERMINATED) {
          break;
        }
        
        // Esperar a ser despertado
        threadMonitor.wait();
      }
    }
  }

  private void executeCPUBurst(Burst burst) throws InterruptedException {
    while (!burst.isCompleted() && running) {
      ProcessState state = process.getState();
      
      // Solo ejecutar si está en RUNNING
      if (state != ProcessState.RUNNING) {
        break;
      }
      
      // Verificar memoria ANTES de ejecutar
      if (!syncController.hasAllRequiredPages(process)) {
        handleMemoryLack();
        return; // Salir de la ejecución
      }
      
      // Ejecutar 1 unidad de CPU
      executeOneCPUUnit(burst);
      
      // Esperar siguiente ciclo (si la ráfaga no terminó)
      if (!burst.isCompleted() && process.getState() == ProcessState.RUNNING) {
        waitForNextCycle();
      }
    }
    
    // Log de ráfaga completada
    if (burst.isCompleted()) {
      int currentTime = syncController.getScheduler().getCurrentTime();
      Logger.exeLog(String.format("[T=%d] [%s] ✓ Ráfaga CPU completada (%d unidades)", 
        currentTime, process.getPid(), burst.getDuration()));
    }
  }

  private void executeOneCPUUnit(Burst burst) {
    int currentTime = syncController.getScheduler().getCurrentTime();
    
    // Ejecutar 1 unidad
    burst.execute(1);
    
    // Log del progreso
    int progress = burst.getDuration() - burst.getRemainingTime();
    int remaining = burst.getRemainingTime();
    
    Logger.exeLog(String.format("[T=%d] [%s] Ejecutando CPU: %d/%d unidades (restante: %d)", 
      currentTime, process.getPid(), progress, burst.getDuration(), remaining));
  }

  private void waitForNextCycle() throws InterruptedException {
    synchronized(threadMonitor) {
      // Verificar estado dentro del lock
      if (process.getState() == ProcessState.RUNNING && running) {
        threadMonitor.wait(); // Esperar señal del motor
      }
    }
  }


  private void handleMemoryLack() {
    int currentTime = syncController.getScheduler().getCurrentTime();
    Logger.syncLog(String.format("[T=%d] [%s] Falta memoria → BLOCKED_MEMORY", 
      currentTime, process.getPid()));
    
    // Bloquear proceso (SyncController usa su propio lock)
    syncController.blockProcessForMemory(process);
    
    // Forzar que el scheduler libere este proceso
    syncController.getScheduler().setCurrentProcess(null);
  }

  private void executeIOBurst(Burst burst) throws InterruptedException {
    int currentTime = syncController.getScheduler().getCurrentTime();
    Logger.procLog(String.format("[T=%d] [%s] → Solicita I/O (duración: %d unidades)", 
      currentTime, process.getPid(), burst.getDuration()));
    
    // Cambiar estado a BLOCKED_IO (sin locks externos)
    if (process.getState() != ProcessState.TERMINATED) {
      process.setState(ProcessState.BLOCKED_IO);
    }
    

    if (process.getState() == ProcessState.BLOCKED_IO) {
      syncController.getScheduler().setCurrentProcess(null);
      ioManager.requestIO(process, burst);
    }
  }

  private void handleBurstCompletion(Burst currentBurst) {
    if (!currentBurst.isCompleted()) {
      return; // Ráfaga aún no termina
    }
    
    ProcessState state = process.getState();
    if (state == ProcessState.TERMINATED) {
      return; // Proceso ya terminó
    }
    
    // Avanzar a siguiente ráfaga
    process.advanceBurst();
    Burst nextBurst = process.getCurrentBurst();
    
    if (nextBurst == null) {
      return; // No hay más ráfagas
    }
    
    if (nextBurst.isCPU() && state != ProcessState.RUNNING) {
      syncController.notifyProcessReady(process, "siguiente ráfaga CPU");
    }
  }


  private void terminateProcess() {
    // Obtener tiempo de finalización
    int currentTime = syncController.getScheduler().getCurrentTime();
    
    // Cambiar estado localmente
    synchronized(threadMonitor) {
      process.setCompletionTime(currentTime);
      process.setState(ProcessState.TERMINATED);
      running = false;
    }
    
    // Liberar recursos (fuera del lock local)
    syncController.releaseProcessResources(process);
    
    // Mostrar métricas finales
    printFinalMetrics();
  }


  private void printFinalMetrics() {
    System.out.println();
    Logger.syncLog("METRICAS FINALES: " + process.getPid() + " ===");
    Logger.syncLog("  Tiempo de espera:  " + process.getWaitingTime());
    Logger.syncLog("  Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.syncLog("  Tiempo respuesta:  " + process.getResponseTime());
    Logger.syncLog("  Fallos de página:  " + process.getPageFaults());
    System.out.println();
  }

  public void wakeUp() {
    synchronized(threadMonitor) {
      threadMonitor.notifyAll();
    }
  }

  public void stopThread() {
    synchronized(threadMonitor) {
      running = false;
      threadMonitor.notifyAll();
    }
  }

  public Process getProcess() { return process; }
}
