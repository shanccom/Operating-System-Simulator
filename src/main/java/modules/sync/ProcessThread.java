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
  private final Object threadMonitor = new Object();
  private volatile boolean running;
  private final Config config;

  public ProcessThread(Process process, SyncController syncController, IOManager ioManager, Config config) {
    super("Thread-" + process.getPid());
    this.process = process;
    this.syncController = syncController;
    this.ioManager = ioManager;
    this.running = true;
    this.config = config;
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
    
  }

  private void mainExecutionLoop() throws InterruptedException {
    
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
        }
      }
    }
    
    terminateProcess();
  }

  private void waitForRunningState() throws InterruptedException {
    synchronized(threadMonitor) {
      while(running && process.getState() != ProcessState.RUNNING && process.getState() != ProcessState.TERMINATED) {
        threadMonitor.wait();
      }
    }
  }

  private void executeCPUBurst(Burst burst) throws InterruptedException {

    if (process.getResponseTime() == -1) {
        process.markFirstExecution(syncController.getScheduler().getCurrentTime());
    }

    while (!burst.isCompleted() && process.getState() == ProcessState.RUNNING) {
      synchronized(threadMonitor) {
        // Esperar señal del motor
        threadMonitor.wait();
      }

      if (process.getState() == ProcessState.RUNNING && !burst.isCompleted()) {
        burst.execute(1); // Ejecutar 1 unidad
        int currentTime = syncController.getScheduler().getCurrentTime();
        Logger.log("[T=" + currentTime + "] [THREAD-" + process.getPid() + "] CPU avanzó 1 unidad (restante: " + burst.getRemainingTime() + ")");
      }
    }

    if (burst.isCompleted()) {
      Logger.log("[THREAD-" + process.getPid() + "] Rafaga CPU completada");
    }
  }

  private void executeIOBurst(Burst burst) throws InterruptedException {
    Logger.log("[THREAD-" + process.getPid() + "] Iniciando I/O (" + burst.getDuration() + " unidades)");
    
    synchronized(threadMonitor) {
      process.setState(ProcessState.BLOCKED_IO);
    }
    
    ioManager.requestIO(process, burst);
    
  }

  private void terminateProcess() {
    
    synchronized(threadMonitor) {
      int currentTime = syncController.getScheduler().getCurrentTime();
      process.setCompletionTime(currentTime);
      process.setState(ProcessState.TERMINATED);
    }
    
    
    syncController.releaseProcessResources(process);
    System.out.println();
    Logger.log("[THREAD-" + process.getPid() + "] METRICAS FINALES:");
    Logger.log("  Tiempo de espera: " + process.getWaitingTime());
    Logger.log("  Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.log("  Fallos de página: " + process.getPageFaults());
    
    synchronized(threadMonitor) {
      running = false;
    }
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

  public Process getProcess() {
    return process;
  }
}