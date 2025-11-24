package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger

public class ProcessThread extends Thread {

  private final Process process;
  private final SyncController syncController;
  private volatile boolean running;

  public ProcessThread (Process process, SyncController, syncController) {
    super("Thread-" + process.getPid());

    this.process = process;
    this.syncController = syncController;
    this.running = true;

    Logger.log("Thread creado para proceso " + process.getPid());
  }

}