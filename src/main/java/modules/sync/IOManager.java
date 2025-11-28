package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class IOManager implements Runnable {
  
  private static class IORequest {
    final Process process;
    final Burst ioBurst;
    final int requestTime;

    IORequest(Process process, Burst ioBurst, int requestTime) {
      this.process = process;
      this.ioBurst = ioBurst;
      this.requestTime = requestTime;
    }
  }

  private final BlockingQueue<IORequest> ioQueue;
  private final SyncController syncController;
  private final Object ioMonitor = new Object();
  private volatile boolean running;
  private Thread ioThread;
  private final AtomicInteger totalIOOperations;
  private final AtomicInteger completedIOOperations;
  private int totalIOTime;

  public IOManager(SyncController syncController) {
    this.ioQueue = new LinkedBlockingQueue<>();
    this.syncController = syncController;
    this.running = false;
    this.totalIOOperations = new AtomicInteger(0);
    this.completedIOOperations = new AtomicInteger(0);
    this.totalIOTime = 0;
  }

  public void start() {
    synchronized(ioMonitor) {
      if (running) {
        return;
      }
      
      running = true;
      ioThread = new Thread(this, "IOManager-Thread");
      ioThread.setDaemon(true);
      ioThread.start();
      
    }
  }

  @Override 
  public void run() {
    while(isRunning()) {
      try {
        IORequest request = ioQueue.take();
        processIORequest(request);
      } catch (InterruptedException e) {
        if (isRunning()) {
          Logger.warning("[IOMANAGER] Interrumpido inesperadamente");
        }
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        Logger.error("[IOMANAGER] Error: " + e.getMessage());
        e.printStackTrace();
      }
    }
    
  }

  private void processIORequest(IORequest request) throws InterruptedException {
    Process process = request.process;
    Burst ioBurst = request.ioBurst;
    int duration = ioBurst.getDuration();

    if (process.getState() == ProcessState.TERMINATED) {
      return;
    }

    int startTime = syncController.getScheduler().getCurrentTime();
    int endTime = startTime + duration;

    Logger.log("[T=" + startTime + "] [IOMANAGER] Iniciando I/O para " + process.getPid() + " (duración: " + duration + ")");

    // Espera sincronizada con el tiempo simulado
    while(isRunning() && process.getState() != ProcessState.TERMINATED) {
      int currentSimTime = syncController.getScheduler().getCurrentTime();
      
      if (currentSimTime >= endTime) {
        break; // I/O completada
      }
      
      // Esperar sincronizadamente con el motor
      synchronized(this) {
        wait(50); // Pequeña espera para no saturar CPU
      }
    }

    if(isRunning() && process.getState() != ProcessState.TERMINATED) {
      synchronized(ioMonitor) {
        ioBurst.execute(duration);
        process.advanceBurst();
        completedIOOperations.incrementAndGet();
        totalIOTime += duration;
      }
      
      int completionTime = syncController.getScheduler().getCurrentTime();
      Logger.log("[T=" + completionTime + "] [IOMANAGER] I/O completada para " + process.getPid() + " (solicitada: " + duration + " unidades)");
      
      // Notificar al SyncController que el proceso volvió a READY
      syncController.notifyProcessReady(process, "completó I/O");
    }
  }

  public void requestIO(Process process, Burst ioBurst) {
    if (!isRunning()) {
      return;
    }

    if (!ioBurst.isIO()) {
      return;
    }

    synchronized(ioMonitor) {
      int currentTime = syncController.getScheduler().getCurrentTime();
      IORequest request = new IORequest(process, ioBurst, currentTime);

      try {
        ioQueue.put(request);
        totalIOOperations.incrementAndGet();
        Logger.log("[THREAD-" + process.getPid() + " → IOMANAGER] Solicitud encolada (cola: " + ioQueue.size() + ")");
      } catch (InterruptedException e) {
        Logger.error("[IOMANAGER] Error encolando: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
  }

  public void stop() {
    synchronized(ioMonitor) {
      if(!running) {
        return;
      }
      
      running = false;
    }

    if (ioThread != null && ioThread.isAlive()) {
      ioThread.interrupt();
      
      try {
        ioThread.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  
    if (!ioQueue.isEmpty()) {
    }
    
  }

  public synchronized boolean isRunning() {
    return running;
  }

  public int getPendingRequests() {
    return ioQueue.size();
  }

  public IOStatistics getStatistics() {
    synchronized(ioMonitor) {
      return new IOStatistics(
        totalIOOperations.get(), 
        completedIOOperations.get(), 
        totalIOTime, 
        ioQueue.size()
      );
    }
  }

  public static class IOStatistics {
    public final int totalRequests;
    public final int completedRequests;
    public final int totalTime;
    public final int pendingRequests;
    
    public IOStatistics(int total, int completed, int totalTime, int pending) {
      this.totalRequests = total;
      this.completedRequests = completed;
      this.totalTime = totalTime;
      this.pendingRequests = pending;
    }
    
    @Override
    public String toString() {
      return String.format(
        "IOStatistics[Total=%d, Completed=%d, Pending=%d, TotalTime=%d]",
        totalRequests, completedRequests, pendingRequests, totalTime
      );
    }
  }
}