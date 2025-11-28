package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger;

public class ProcessThread extends Thread {

  private final Process process;
  private final SyncController syncController;
  private final IOManager ioManager;
  private final Object threadMonitor = new Object();
  private volatile boolean running;

  public ProcessThread(Process process, SyncController syncController, IOManager ioManager) {
    super("Thread-" + process.getPid());
    this.process = process;
    this.syncController = syncController;
    this.ioManager = ioManager;
    this.running = true;
  }

  @Override 
  public void run() {
    try {
      waitForArrival();
      arriveAtSystem();
      mainExecutionLoop();
      
    } catch (InterruptedException e) {
      Logger.warning("[THREAD-" + process.getPid() + "] Interrumpido");
      Thread.currentThread().interrupt();
    }
  }

  private void waitForArrival() throws InterruptedException {
    int arrivalTime = process.getArrivalTime();
    Logger.debug("[THREAD-" + process.getPid() + "] Esperando tiempo de llegada t=" + arrivalTime);
    
    synchronized(threadMonitor) {
      while(running && syncController.getScheduler().getCurrentTime() < arrivalTime) {
        threadMonitor.wait(50);
      }
    }
    Logger.log("LLEGADA: " + process.getPid() + " alcanzo su tiempo (t=" + arrivalTime + ")");
  }

  private void arriveAtSystem() {    
    synchronized(threadMonitor) {
      process.setState(ProcessState.READY);
    }
    
    syncController.notifyProcessReady(process, "llegada al sistema");
    
    Logger.log("[SYNC → THREAD-" + process.getPid() + "] Proceso agregado a cola READY");
  }

  private void mainExecutionLoop() throws InterruptedException {
    Logger.log("[THREAD-" + process.getPid() + "] Entrando a ciclo de ejecucion");
    
    while (running && !process.isCompleted()) {
      // El thread espera hasta que el Scheduler lo seleccione
      waitForRunningState();

      if (!running || process.getState() == ProcessState.TERMINATED) {
        break;
      }

      Burst currentBurst = process.getCurrentBurst();

      if (currentBurst == null) {
        Logger.log("[THREAD-" + process.getPid() + "] No hay mas rafagas, terminando");
        break;
      }

      if (currentBurst.isCPU()) {
        executeCPUBurst(currentBurst);
      } else {
        executeIOBurst(currentBurst);
      }

      synchronized(threadMonitor) {
        if (currentBurst.isCompleted()) {
          process.advanceBurst();
          Logger.log("[THREAD-" + process.getPid() + "] Rafaga completada, avanzando");
        }
      }
    }
    
    terminateProcess();
  }

  private void waitForRunningState() throws InterruptedException {
    Logger.log("[THREAD-" + process.getPid() + "] Esperando turno del Scheduler...");
    
    synchronized(threadMonitor) {
      while(running && process.getState() != ProcessState.RUNNING && process.getState() != ProcessState.TERMINATED) {
        Logger.log("[THREAD-" + process.getPid() + "] Estado: " + process.getState() + " → wait()");
        threadMonitor.wait();
        Logger.log("[THREAD-" + process.getPid() + "] ↑ Despertado! Estado: " + process.getState());
      }
    }
  }

  private void executeCPUBurst(Burst burst) throws InterruptedException {
    int currentTime = syncController.getScheduler().getCurrentTime();
    
    synchronized(threadMonitor) {
      process.markFirstExecution(currentTime);
    }
    
    Logger.log("[THREAD-" + process.getPid() + "] Ejecutando CPU (restante: " + burst.getRemainingTime() + ")");

    while (!burst.isCompleted() && process.getState() == ProcessState.RUNNING) {
      synchronized(threadMonitor) {
        burst.execute(1);
        syncController.getScheduler().recordCPUTime(1);
        Logger.log("[THREAD-" + process.getPid() + "] CPU tick (restante: " + burst.getRemainingTime() + ")");
      }
      
      Thread.sleep(100);

      if (process.getState() != ProcessState.RUNNING) {
        Logger.log("[THREAD-" + process.getPid() + "] ⚠️  Preemption detectada!");
        break;
      }
    }

    if (burst.isCompleted()) {
      Logger.log("[THREAD-" + process.getPid() + "] Rafaga CPU completada");
    }
  }

  private void executeIOBurst(Burst burst) throws InterruptedException {
    Logger.log("[THREAD-" + process.getPid() + "] Iniciando I/O (" + burst.getDuration() + " unidades)");
    Logger.log("[THREAD-" + process.getPid() + " → SYNC] Notificando bloqueo por I/O");
    
    synchronized(threadMonitor) {
      process.setState(ProcessState.BLOCKED_IO);
    }
    
    // El thread se auto-bloquea y delega la operación I/O al IOManager
    Logger.log("[THREAD-" + process.getPid() + " → IOMANAGER] Delegando operación I/O");
    ioManager.requestIO(process, burst);
    
    Logger.log("[THREAD-" + process.getPid() + "] Thread bloqueado, esperando I/O...");
  }

  private void terminateProcess() {
    Logger.log("THREAD-" + process.getPid() + " FINALIZANDO");
    
    synchronized(threadMonitor) {
      int currentTime = syncController.getScheduler().getCurrentTime();
      process.setCompletionTime(currentTime);
      process.setState(ProcessState.TERMINATED);
    }
    
    
    syncController.releaseProcessResources(process);
    System.out.println();
    Logger.log("[THREAD-" + process.getPid() + "] METRICAS FINALES:");
    Logger.log("  • Tiempo de espera: " + process.getWaitingTime());
    Logger.log("  • Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.log("  • Fallos de página: " + process.getPageFaults());
    
    synchronized(threadMonitor) {
      running = false;
    }
    System.out.println();
  }

  public void wakeUp() {
    synchronized(threadMonitor) {
      Logger.log("[MOTOR → THREAD-" + process.getPid() + "] Enviando señal de despertar");
      threadMonitor.notifyAll();
    }
  }

  public void stopThread() {
    synchronized(threadMonitor) {
      running = false;
      threadMonitor.notifyAll();
    }
  }

  public Process getProcess() {
    return process;
  }
}