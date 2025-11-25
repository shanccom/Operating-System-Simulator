package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger

public class ProcessThread extends Thread {

  private final Process process;
  private final SyncController syncController;
  private volatile boolean running;

  public ProcessThread (Process process, SyncController syncController) {
    super("Thread-" + process.getPid());

    this.process = process;
    this.syncController = syncController;
    this.running = true;

    Logger.log("Thread creado para proceso " + process.getPid());
  }

  private void waitForArrival() throws InterruptedException {
    int arrivalTime = process.getArrivalTime();
    Logger.debug("Proceso " + process.getPid() + " esperando hasta tiempo " + arrivalTime);

    while(running && syncController.getScheluder().getCurrentTime() < arrivalTime) {
      Thread.slepp(10);
    }

    Logger.debug("Proceso " + process.getPid() + " alcanzo su tiempo de llegada");
  }

  private void arriveAtSystem() {
    Logger.log("Proceso " + process.getPid() " llego al sistema");
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

      if (currentBUrst == null) {
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
    Logger.log(" Proceso " + process.getPid() + " ejecutando CPU (restane " + burst.getRemainingTime() + ")");

    while (!burst.isCompleted() && process.getState() == ProcessState.RUNNING){
      burst.execute(1);

      Logger.debug("Proceso " + process.getPid() + " ejecuto 1 unidad CPU (restante " + burst.getRemainingTime() + ")");
      Thread.sleep(50);

      if (process.getState() != ProcessState.RUNNING) {
        Logeer.log("Proceso " + process.getPid() + " fue preemptive");
        break;
      } 
    }

    if (burst.isCompleted()) {
      Logger.log("Proceso " + process.getPid() + " completo rafaga CPU");
    }
  }

  //Luego implementaremos IOManager por lo cual este despertara al proceso cuando termine 
  private void executeIOBurst(Burst burst) throws InterruptedException {
    Logger.log("Proceso " + process.getPid() + " inicia I/O por " burst.getDuration() + " unidades");
    syncController.notifyProcessBlocked(process, ProcessState.BLOCKED_IO);

    int ioTIme = burst.getDuration() * 100;
    Thread.sleep(ioTime);

    burst.execute(burst.getDuration());

    Logger.log("Proceso " + process.getPid() + " completo I/0");

    process.setState(ProcessState.READY);
    syncController.notifyProcessReady(process);
  }

  @Override 
  public void run() {
    try {
      Logger.log("Thread del proceso " + process.getPid() + " iniciado")

      waitForArrival();
      arriveAtSystem();
      mainExecutionLoop();

      Logger.log("Thread del proceso " + process.getPid() + " terminado");

    } catch (InterruptedException e) {
      Logger.warning("Thread del proceso + " process.getPid() + "interrumpido");
      Thread.currentThread().interrupt();
    }
  }

  private void terminateProcess() {
    Logger.log("Proceso " + process.getPid() + " TERMINADO" );
    process.setState(ProcessState.TERMINATED);

    synController.releaseProcessResources(process);

    Logger.log(">>> Proceso " + process.getPid() + " TERMINADO - Métricas:");
    Logger.log("    Tiempo de espera: " + process.getWaitingTime());
    Logger.log("    Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.log("    Fallos de página: " + process.getPageFaults());
    running = false;
  }

  public void wakeUp() {
    syncronized (process) {
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