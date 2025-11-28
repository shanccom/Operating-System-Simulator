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
        Logger.warning("[IOMANAGER] Ya está ejecutándose");
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
    
    Logger.log("[IOMANAGER] Thread terminado");
  }

  private void processIORequest(IORequest request) throws InterruptedException {
    Process process = request.process;
    Burst ioBurst = request.ioBurst;
    int duration = ioBurst.getDuration();

    if (process.getState() == ProcessState.TERMINATED) {
      Logger.warning("[IOMANAGER] Ignorando solicitud de proceso terminado: " + process.getPid());
      return;
    }

    int currentTime = syncController.getScheduler().getCurrentTime();
    int ioTimeMs = duration * 100;
    int steps = duration;
    int sleepPerStep = ioTimeMs / steps;

    // Simular operación I/O con delays
    for(int i = 0; i < steps && isRunning(); i++) {
      Thread.sleep(sleepPerStep);
      Logger.log("[IOMANAGER] " + process.getPid() + " I/O progreso: " + (i+1) + "/" + steps);
      
      if (process.getState() == ProcessState.TERMINATED) {
        Logger.warning("[IOMANAGER] Proceso terminó durante I/O, abortando");
        return;
      }
    }

    // I/O completada
    if(isRunning() && process.getState() != ProcessState.TERMINATED) {
      synchronized(ioMonitor) {
        ioBurst.execute(duration);
        process.advanceBurst();
        completedIOOperations.incrementAndGet();
        totalIOTime += duration;
      }
      
      int completionTime = syncController.getScheduler().getCurrentTime();
      int waitTime = completionTime - currentTime;

      Logger.log("[IOMANAGER] I/O completada para " + process.getPid() + " (duró " + waitTime + " unidades)");
      Logger.log("[IOMANAGER → SYNC] Notificando que " + process.getPid() + " está listo");
      
      // Notificar al SyncController que el proceso volvió a READY
      process.setState(ProcessState.READY);
      syncController.notifyProcessReady(process, "completó I/O");
      
      Logger.log("[SYNC → SCHEDULER] Proceso " + process.getPid() + " agregado a cola READY");
    }
  }

  public void requestIO(Process process, Burst ioBurst) {
    if (!isRunning()) {
      Logger.error("[IOMANAGER] No puede procesar I/O, no está ejecutándose");
      return;
    }

    if (!ioBurst.isIO()) {
      Logger.error("[IOMANAGER] Error: ráfaga no es de tipo I/O");
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
      
      Logger.log("[IOMANAGER] Deteniéndose...");
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
      Logger.warning("[IOMANAGER] Detenido con " + ioQueue.size() + " solicitudes pendientes");
    }
    
    Logger.log("[IOMANAGER] Detenido");
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

  public void printMetrics() {
    Logger.log("MÉTRICAS DE ENTRADA/SALIDA (I/O)");
    
    synchronized(ioMonitor) {
      int total = totalIOOperations.get();
      int completed = completedIOOperations.get();
      int pending = ioQueue.size();
      
      Logger.log("Total de operaciones I/O: " + total);
      Logger.log("Operaciones completadas: " + completed);
      Logger.log("Operaciones pendientes: " + pending);
      Logger.log("Tiempo total en I/O: " + totalIOTime + " unidades");
      
      if (completed > 0) {
        double avgTime = (double) totalIOTime / completed;
        Logger.log(String.format("Tiempo promedio I/O: %.2f unidades", avgTime));
      }
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