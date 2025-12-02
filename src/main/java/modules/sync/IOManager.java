package modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger;
import model.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class IOManager implements Runnable {
  
  private static class IORequest {
    final Process process;
    final Burst ioBurst;
    

    IORequest(Process process, Burst ioBurst, int requestTime) {
      this.process = process;
      this.ioBurst = ioBurst;
    }
  }

  private final BlockingQueue<IORequest> ioQueue;
  
  private final SyncController syncController;
  
  private final AtomicBoolean running;
  private Thread ioThread;
  
  private final Object ioMonitor = new Object();
  
  private final AtomicInteger totalIOOperations;
  private final AtomicInteger completedIOOperations;
  private final AtomicInteger totalIOTime;

  private final Config config;
  
  private SimulationStateListener stateListener;

  public IOManager(SyncController syncController, Config config) {
    this.syncController = syncController;
    this.ioQueue = new LinkedBlockingQueue<>();
    this.running = new AtomicBoolean(false);
    this.totalIOOperations = new AtomicInteger(0);
    this.completedIOOperations = new AtomicInteger(0);
    this.totalIOTime = new AtomicInteger(0);
    this.config = config;
  }

  // metodo para establecer el listener
  public void setStateListener(SimulationStateListener listener) {
      this.stateListener = listener;
    }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      Logger.warning("[IOMANAGER] Ya está en ejecución");
      return;
    }
    
    ioThread = new Thread(this, "IOManager-Thread");
    ioThread.setDaemon(true);
    ioThread.start();
    
    Logger.syncLog("[IOMANAGER] Iniciado correctamente");
  }

  @Override 
  public void run() {
    Logger.syncLog("[IOMANAGER] Thread I/O iniciado");
    
    while(isRunning()) {
      try {
        // BlockingQueue.take() es thread-safe, espera indefinidamente
        IORequest request = ioQueue.take();
        
        // Procesar la solicitud
        processIORequest(request);
        
      } catch (InterruptedException e) {
        if (isRunning()) {
          Logger.warning("[IOMANAGER] Interrumpido inesperadamente");
        }
        Thread.currentThread().interrupt();
        break;
        
      } catch (Exception e) {
        Logger.error("[IOMANAGER] Error procesando I/O: " + e.getMessage());
        e.printStackTrace();
      }
    }
    
    Logger.syncLog("[IOMANAGER] Thread I/O detenido");
  }

  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }
    
    Logger.syncLog("[IOMANAGER] Deteniendo...");
    
    synchronized(ioMonitor) {
      ioMonitor.notifyAll();
    }
    
    if (ioThread != null && ioThread.isAlive()) {
      ioThread.interrupt();
      
      try {
        ioThread.join(2000);
        
        if (ioThread.isAlive()) {
          Logger.warning("[IOMANAGER] Thread no terminó después del timeout");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Logger.warning("[IOMANAGER] Interrumpido durante stop");
      }
    }
    
    int pending = ioQueue.size();
    if (pending > 0) {
      Logger.warning("[IOMANAGER] Detenido con " + pending + " solicitudes pendientes");
    } else {
      Logger.syncLog("[IOMANAGER] Detenido correctamente");
    }
  }

  public void requestIO(Process process, Burst ioBurst) {
    if (!isRunning()) {
      Logger.warning("[IOMANAGER] Ignorando solicitud (no está corriendo)");
      return;
    }

    if (!ioBurst.isIO()) {
      Logger.warning("[IOMANAGER] Ráfaga no es de tipo I/O");
      return;
    }

    int currentTime;
    synchronized(syncController.getCoordinationMonitor()) {
      currentTime = syncController.getCurrentTime();
    }
    
    int systemCallOverhead = config.getSystemCallOverhead();
    int endTime = currentTime + systemCallOverhead;

    synchronized(syncController.getCoordinationMonitor()) {
      process.setSystemCallEndTime(endTime);
    }
    
    IORequest request = new IORequest(process, ioBurst, currentTime);
    
    try {
      // BlockingQueue.put() es thread-safe
      ioQueue.put(request);
      totalIOOperations.incrementAndGet();
      
      Logger.procLog(String.format("[T=%d] [%s] I/O encolada (pendientes: %d)", 
        currentTime, process.getPid(), ioQueue.size()));
        
    } catch (InterruptedException e) {
      Logger.error("[IOMANAGER] Error encolando solicitud: " + e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private void processIORequest(IORequest request) throws InterruptedException {
    Process process = request.process;
    Burst ioBurst = request.ioBurst;
    int duration = ioBurst.getDuration();

    synchronized(syncController.getCoordinationMonitor()) {
      if (process.getState() == ProcessState.TERMINATED) {
        Logger.warning("[IOMANAGER] Proceso " + process.getPid() + " ya terminó");
        return;
      }
    }

    // Esperar que system call complete
    waitForSystemCallCompletion(process);

    int startTime;
    synchronized(syncController.getCoordinationMonitor()) {
      startTime = syncController.getCurrentTime();
    }
    int endTime = startTime + duration;

    Logger.procLog(String.format("[T=%d] [I/O] Procesando I/O para %s (duración: %d, fin: t=%d)", 
      startTime, process.getPid(), duration, endTime));
    
    //INICIO para gant
    // Notificar inicio de I/O
    if (stateListener != null) {
        stateListener.onIOStarted(process.getPid(), startTime);
    }
    //FIN

    // Esperar hasta que el tiempo simulado alcance endTime
    waitUntilIOCompletes(process, endTime);

    // Completar la operación
    completeIOOperation(process, ioBurst, duration);
  }

  private void waitUntilIOCompletes(Process process, int endTime) throws InterruptedException {
    while(isRunning()) {
      int currentSimTime;
      ProcessState processState;
      
      synchronized(syncController.getCoordinationMonitor()) {
        currentSimTime = syncController.getCurrentTime();
        processState = process.getState();
      }
      
      // Si el proceso fue terminado o el tiempo ya pasó
      if (processState == ProcessState.TERMINATED || currentSimTime >= endTime) {
        break;
      }
      
      // Esperar un poco (polling aceptable en simulación)
      synchronized(ioMonitor) {
        ioMonitor.wait(50);
      }
    }
  }

  private void completeIOOperation(Process process, Burst ioBurst, int duration) {
    synchronized(syncController.getCoordinationMonitor()) {
      ProcessState state = process.getState();
      
      if (!isRunning() || state == ProcessState.TERMINATED) {
        return;
      }
      
      // Marcar la ráfaga como ejecutada
      ioBurst.execute(duration);
      
      // Avanzar a la siguiente ráfaga
      process.advanceBurst();
      
      // Obtener tiempo actual
      int completionTime = syncController.getCurrentTime();
      
      Logger.procLog(String.format("[T=%d] [I/O] I/O completada para %s (duración: %d unidades)", 
        completionTime, process.getPid(), duration));
      
      //INICIO para gant
      //notifica fin de I/O
      if (stateListener != null) {
          stateListener.onIOEnded(process.getPid(), completionTime);
      }
      //FIN


      // Actualizar estadísticas (AtomicInteger es thread-safe)
      completedIOOperations.incrementAndGet();
      totalIOTime.addAndGet(duration);
    }
    
    syncController.notifyProcessReady(process, "completó I/O");
  }

  private void waitForSystemCallCompletion(Process process) throws InterruptedException {
    while(isRunning()) {
      boolean inSystemCall;
      int endTime;
      int currentSimTime;
      ProcessState processState;
      
      synchronized(syncController.getCoordinationMonitor()) {
        inSystemCall = process.isInSystemCall();
        endTime = process.getSystemCallEndTime();
        currentSimTime = syncController.getCurrentTime();
        processState = process.getState();
      }
      
      // Si no hay system call pendiente, salir
      if (!inSystemCall) {
        break;
      }
      
      // Si el proceso fue terminado
      if (processState == ProcessState.TERMINATED) {
        break;
      }
      
      // Si el system call ya completó
      if (currentSimTime >= endTime) {

        synchronized(syncController.getCoordinationMonitor()) {
          process.clearSystemCall();
        }
        break;
      }
      
      // Esperar un poco
      synchronized(ioMonitor) {
        ioMonitor.wait(50);
      }
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public int getPendingRequests() {
    return ioQueue.size();
  }

  public IOStatistics getStatistics() {
    return new IOStatistics(
      totalIOOperations.get(), 
      completedIOOperations.get(), 
      totalIOTime.get(), 
      ioQueue.size()
    );
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
    
    public double getAverageIOTime() {
      if (completedRequests == 0) {
        return 0.0;
      }
      return (double) totalTime / completedRequests;
    }
    
    public double getCompletionRate() {
      if (totalRequests == 0) {
        return 100.0;
      }
      return (completedRequests * 100.0) / totalRequests;
    }
    
    @Override
    public String toString() {
      return String.format(
        "IOStatistics[Total=%d, Completed=%d, Pending=%d, TotalTime=%d, Avg=%.2f]",
        totalRequests, completedRequests, pendingRequests, totalTime, getAverageIOTime()
      );
    }
  }
}