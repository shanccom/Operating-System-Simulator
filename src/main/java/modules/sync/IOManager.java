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
  private volatile int totalIOTime;

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

    // Verificar estado antes de procesar
    if (process.getState() == ProcessState.TERMINATED) {
      return;
    }

    int startTime = syncController.getScheduler().getCurrentTime();
    int endTime = startTime + duration;

    Logger.log(String.format("[T=%d] [I/O] Iniciando operacion I/O para %s (duracion: %d unidades, completara en t=%d)", 
        startTime, process.getPid(), duration, endTime));

    // Espera sincronizada con el tiempo simulado
    // Usar ioMonitor consistentemente en lugar de synchronized(this)
    while(isRunning() && process.getState() != ProcessState.TERMINATED) {
      int currentSimTime = syncController.getScheduler().getCurrentTime();
      
      if (currentSimTime >= endTime) {
        break; // I/O completada
      }
      
      // Esperar sincronizadamente con el motor usando ioMonitor
      synchronized(ioMonitor) {
        if (isRunning() && process.getState() != ProcessState.TERMINATED && 
            syncController.getScheduler().getCurrentTime() < endTime) {
          ioMonitor.wait(50); // Pequeña espera para no saturar CPU
        } else {
          break;
        }
      }
    }

    // Completar I/O solo si el proceso sigue activo
    if(isRunning() && process.getState() != ProcessState.TERMINATED) {
      // Ejecutar burst y actualizar estadísticas de forma atómica
      synchronized(ioMonitor) {
        if (process.getState() != ProcessState.TERMINATED) {
          ioBurst.execute(duration);
          process.advanceBurst();
          completedIOOperations.incrementAndGet();
          totalIOTime += duration;
        }
      }
      
      // Notificar fuera del lock para evitar deadlocks con SyncController
      if (process.getState() != ProcessState.TERMINATED) {
        int completionTime = syncController.getScheduler().getCurrentTime();
        Logger.log(String.format("[T=%d] [I/O]    Operacion I/O completada para %s (duracion: %d unidades)", 
            completionTime, process.getPid(), duration));
        
        // Notificar al SyncController que el proceso volvió a READY
        // Esto se hace fuera del ioMonitor para evitar deadlocks
        syncController.notifyProcessReady(process, "completó I/O");
      }
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
        Logger.log(String.format("[T=%d] [%s → I/O] Solicitud encolada (solicitudes pendientes: %d)", 
            currentTime, process.getPid(), ioQueue.size()));
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
      // Despertar el thread si está esperando
      ioMonitor.notifyAll();
    }

    if (ioThread != null && ioThread.isAlive()) {
      ioThread.interrupt();
      
      try {
        ioThread.join(2000);
        if (ioThread.isAlive()) {
          Logger.warning("[IOMANAGER] El thread no terminó después del timeout");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Logger.error("[IOMANAGER] Interrumpido durante stop");
      }
    }
    
    // Log de solicitudes pendientes si las hay
    int pending = ioQueue.size();
    if (pending > 0) {
      Logger.warning("[IOMANAGER] Detenido con " + pending + " solicitudes pendientes en cola");
    }
  }

  public boolean isRunning() {
    // running es volatile, no necesita sincronización
    return running;
  }

  public int getPendingRequests() {
    return ioQueue.size();
  }

  public IOStatistics getStatistics() {
    // Leer valores de forma atómica para snapshot consistente
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