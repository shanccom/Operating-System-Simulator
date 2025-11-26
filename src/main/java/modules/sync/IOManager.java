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

    @Override
    public String toString() {
      return String.format("IORequest[%s, duracion=%d]", process.getPid(), ioBurst.getDuration());
    }
  }

  private final BlockingQueue<IORequest> ioQueue;
  private final SyncController syncController;
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

    Logger.log("IOManager inicializado");
  }


  public void start() {
    if (running) {
      Logger.warning("IOManager ya se esta ejecutando");
      return;
    }

    running = true;
    ioThread = new Thread(this, "IOManager Thread");
    ioThread.setDaemon(true);
    ioThread.start();

    Logger.log("IOManager iniciado con thread independiente");
  }

  @Override 
  public void run() {
    Logger.log("IOManager thread ejecutandose");

    while(running) {
      try{
        IORequest request = ioQueue.take();
        processIORequest(request);
      } catch (InterruptedException e) {
        if (running) {
          Logger.warning("IOManager interrumpido inesperadamente");
        }
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        Logger.error("Error en IOManager: "  + e.getMessage());
        e.printStackTrace();
      }
    }
    Logger.log("IOManager thread terminado");
  }

  private void processIORequest(IORequest request) throws InterruptedException {
    Process process = request.process;
    Burst ioBurst = request.ioBurst;
    int duration = ioBurst.getDuration();

    if (process.getState() == ProcessState.TERMINATED) {
      Logger.warning("IOManager ignora solicitud de procesos terminado " + process.getPid());
      return;
    }

    Logger.log("[SYNC] IOManager tomó solicitud de cola bloqueante");
    Logger.log(String.format("IOManager procesando I/O de %s por %d unidades", process.getPid(), duration));
    
    int currentTime = syncController.getScheduler().getCurrentTime();
    int ioTimeMs = duration * 100;
    int steps = duration;
    int sleepPerStep = ioTimeMs / steps;

    for(int i = 0; i < steps && running; i++){
      Thread.sleep(sleepPerStep);
      Logger.debug(String.format("IOManager: %s I/O progreso %d/%d", process.getPid(), i+1, steps ));
      
      if (process.getState() == ProcessState.TERMINATED) {
        Logger.warning("Proceso " + process.getPid() + " terminó durante I/O, abortando operación");
        return;
      }

    }

    if(running && process.getState() != ProcessState.TERMINATED) {
      ioBurst.execute(duration);
      process.advanceBurst();
      completedIOOperations.incrementAndGet();
      totalIOTime += duration;
      int completionTime = syncController.getScheduler().getCurrentTime();
      int waitTime = completionTime - currentTime;

      Logger.log(String.format("IOManager completo I/O de %s (espero %d unidades)", process.getPid(), waitTime));
      Logger.log("[SYNC] IOManager notificando proceso listo después de I/O");
      process.setState(ProcessState.READY);
      syncController.notifyProcessReady(process);
      Logger.log(String.format("Proceso %s desbloqueado por I/O, volvió a READY", process.getPid())); 
    }
  }

  public void requestIO(Process process, Burst ioBurst) {
    if (!running){
      Logger.error("No se puede procesar I/O, IOManager no se esta ejecutando");
      return;
    }

    if (!ioBurst.isIO()){
      Logger.error("Se intento enconlar una rafa que no es I/O");
      return;
    }

    int currentTime = syncController.getScheduler().getCurrentTime();
    IORequest request = new IORequest(process, ioBurst, currentTime);

    try {
      ioQueue.put(request);
      totalIOOperations.incrementAndGet();
      Logger.log(String.format("Proceso %s encolo solicitud de IO de %d unidades (Cola %d)", process.getPid(), ioBurst.getDuration(), ioQueue.size()));
    } catch (InterruptedException e) {
      Logger.error("Error encolando solictud I/O " + e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  public void stop() {
    if(!running) {
      return;
    }
    Logger.log("IOManager detenido");
    running = false;

    if (ioThread != null && ioThread.isAlive()) {
      ioThread.interrupt();
      
      try {
        ioThread.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  
    if (!ioQueue.isEmpty()) {
      Logger.warning("IOManager detenido con " + ioQueue.size() + " solicitudes pendientes");
    }
    
    Logger.log("IOManager detenido");
  }

  public boolean isRunning() {
    return running;
  }

  public int getPendingRequest() {
    return ioQueue.size();
  }

  public IOStatistics getStatistics() {
    return new IOStatistics( totalIOOperations.get(), completedIOOperations.get(), totalIOTime, ioQueue.size() );
  }

  public void printMetrics() {
    Logger.separator();
    Logger.section("Metricas de Entrada y Salida");
    
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
    
    Logger.separator();
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