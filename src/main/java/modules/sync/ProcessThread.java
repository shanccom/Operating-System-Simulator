package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger;

public class ProcessThread extends Thread {

  private final Process process;
  private final SyncController syncController;
  private volatile boolean running;
  private final IOManager IoManager;

  public ProcessThread (Process process, SyncController syncController, IOManager IoManager) {
    super("Thread-" + process.getPid());

    this.process = process;
    this.syncController = syncController;
    this.IoManager = IoManager;
    this.running = true;
  }

  private void waitForArrival() throws InterruptedException {
    int arrivalTime = process.getArrivalTime();
    Logger.debug("Proceso " + process.getPid() + " esperando hasta tiempo " + arrivalTime);

    synchronized(process) {
        while(running && syncController.getScheduler().getCurrentTime() < arrivalTime) {
            process.wait(50);
        }
    }

    Logger.debug("Proceso " + process.getPid() + " alcanzo su tiempo de llegada");
  }

  private void arriveAtSystem() {
    Logger.log("Proceso " + process.getPid() + " llego al sistema");
    process.setState(ProcessState.READY);
    syncController.notifyProcessReady(process);
  }

  private void mainExecutionLoop() throws InterruptedException{
    while (running && !process.isCompleted()) {
      waitForRunningState();

      if (!running || process.getState() == ProcessState.TERMINATED){
        break;
      }

      Burst currentBurst = process.getCurrentBurst();

      if (currentBurst == null) {
        Logger.log("Proceso " + process.getPid() + " no tinee mas rafagas");
        break;
      }

      if (currentBurst.isCPU()) {
        executeCPUBurst(currentBurst);
      } else {
        executeIOBurst(currentBurst);
      }

      if (currentBurst.isCompleted()) {
        process.advanceBurst();
        Logger.debug("Proceso " + process.getPid() + " avanzo a la siguiente rafaga");
      }
    }
    terminateProcess();
  }

  private void waitForRunningState() throws InterruptedException {
    synchronized(process) {
      while(running && process.getState() != ProcessState.RUNNING && process.getState() != ProcessState.TERMINATED){
        Logger.debug("Proceso " + process.getPid() + " esperando turno (estado: " + process.getState() + ")");
        process.wait();
      }
      Logger.debug("Proceso " + process.getPid() + " desperto (estado: " + process.getState() + ")");
    }
  }

  private void executeCPUBurst(Burst burst) throws InterruptedException {
    int currentTime = syncController.getScheduler().getCurrentTime();
    process.markFirstExecution(currentTime);
    
    Logger.log(" Proceso " + process.getPid() + " ejecutando CPU (restane " + burst.getRemainingTime() + ")");

    while (!burst.isCompleted() && process.getState() == ProcessState.RUNNING){
      burst.execute(1);

      Logger.debug("Proceso " + process.getPid() + " ejecuto 1 unidad CPU (restante " + burst.getRemainingTime() + ")");
      Thread.sleep(50);

      if (process.getState() != ProcessState.RUNNING) {
        Logger.log("Proceso " + process.getPid() + " fue preemptive");
        break;
      } 
    }

    if (burst.isCompleted()) {
      Logger.log("Proceso " + process.getPid() + " completo rafaga CPU");
    }
  }

  private void executeIOBurst(Burst burst) throws InterruptedException {
    Logger.log("Proceso " + process.getPid() + " inicia I/O por " + burst.getDuration() + " unidades");
    syncController.notifyProcessBlocked(process, ProcessState.BLOCKED_IO);

    IoManager.requestIO(process, burst);
  }

  @Override 
  public void run() {
    try {
      waitForArrival();
      arriveAtSystem();
      mainExecutionLoop();
    } catch (InterruptedException e) {
      Logger.warning("Thread del proceso " + process.getPid() + "interrumpido");
      Thread.currentThread().interrupt();
    }
  }

  private void terminateProcess() {
    int currentTime = syncController.getScheduler().getCurrentTime();
    process.setCompletionTime(currentTime);

    Logger.log("Proceso " + process.getPid() + " completado en t = " + currentTime);
    Logger.log("Proceso " + process.getPid() + " TERMINADO" );
    process.setState(ProcessState.TERMINATED);

    syncController.releaseProcessResources(process);

    Logger.log("Proceso " + process.getPid() + " TERMINADO - Métricas:");
    Logger.log("    Tiempo de espera: " + process.getWaitingTime());
    Logger.log("    Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.log("    Fallos de página: " + process.getPageFaults());
    running = false;
  }

  public void wakeUp() {
    synchronized (process) {
      process.notifyAll();
    }
  }

  public void stopThread() {
    running = false;
    wakeUp();
  }

  public Process getProcess() {
    return process;
  }
}