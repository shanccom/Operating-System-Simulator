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
    final int requestTime;
    

    IORequest(Process process, Burst ioBurst, int requestTime) {
      this.process = process;
      this.ioBurst = ioBurst;
      this.requestTime = requestTime;
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

  public IOManager(SyncController syncController, Config config) {
    this.syncController = syncController;
    this.ioQueue = new LinkedBlockingQueue<>();
    this.running = new AtomicBoolean(false);
    this.totalIOOperations = new AtomicInteger(0);
    this.completedIOOperations = new AtomicInteger(0);
    this.totalIOTime = new AtomicInteger(0);
    this.config = config;
  }

  public void start() {
    // Usar compareAndSet para garantizar que solo se inicie una vez
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
        // Esperar solicitud (bloquea hasta que haya una)
        IORequest request = ioQueue.take();
        
        // Procesar la solicitud
        processIORequest(request);
        
      } catch (InterruptedException e) {
        // Si fue interrumpido y aún está corriendo, es inesperado
        if (isRunning()) {
          Logger.warning("[IOMANAGER] Interrumpido inesperadamente");
        }
        Thread.currentThread().interrupt();
        break;
        
      } catch (Exception e) {
        // Capturar cualquier otra excepción para no detener el thread
        Logger.error("[IOMANAGER] Error procesando I/O: " + e.getMessage());
        e.printStackTrace();
      }
    }
    
    Logger.syncLog("[IOMANAGER] Thread I/O detenido");
  }

  public void stop() {
    // Cambiar estado a no running
    if (!running.compareAndSet(true, false)) {
      return; // Ya estaba detenido
    }
    
    Logger.syncLog("[IOMANAGER] Deteniendo...");
    
    // Despertar el thread si está esperando
    synchronized(ioMonitor) {
      ioMonitor.notifyAll();
    }
    
    // Interrumpir el thread
    if (ioThread != null && ioThread.isAlive()) {
      ioThread.interrupt();
      
      try {
        // Esperar a que termine (máximo 2 segundos)
        ioThread.join(2000);
        
        if (ioThread.isAlive()) {
          Logger.warning("[IOMANAGER] Thread no terminó después del timeout");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Logger.warning("[IOMANAGER] Interrumpido durante stop");
      }
    }
    
    // Reportar solicitudes pendientes
    int pending = ioQueue.size();
    if (pending > 0) {
      Logger.warning("[IOMANAGER] Detenido con " + pending + " solicitudes pendientes");
    } else {
      Logger.syncLog("[IOMANAGER] Detenido correctamente (sin solicitudes pendientes)");
    }
  }

  public void requestIO(Process process, Burst ioBurst) {
    // Verificar que esté corriendo
    if (!isRunning()) {
      Logger.warning("[IOMANAGER] Ignorando solicitud (no está corriendo)");
      return;
    }

    // Verificar que sea una ráfaga I/O válida
    if (!ioBurst.isIO()) {
      Logger.warning("[IOMANAGER] Ráfaga no es de tipo I/O");
      return;
    }

    int currentTime = syncController.getScheduler().getCurrentTime();
    
    int systemCallOverhead = config.getSystemCallOverhead();
    int endTime = currentTime + systemCallOverhead;

    process.setSystemCallEndTime(endTime);
    
    Logger.procLog(String.format("[T=%d] [SYSTEM CALL] Iniciando para %s (overhead: %d ciclos, termina en t=%d)", 
        currentTime, process.getPid(), systemCallOverhead, endTime));

    // Crear solicitud
    IORequest request = new IORequest(process, ioBurst, currentTime);
    
    try {
      // Encolar solicitud (BlockingQueue es thread-safe)
      ioQueue.put(request);
      
      // Incrementar contador (AtomicInteger es thread-safe)
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

    // Verificar que el proceso aún esté activo
    if (process.getState() == ProcessState.TERMINATED) {
      Logger.warning("[IOMANAGER] Proceso " + process.getPid() + " ya terminó, ignorando I/O");
      return;
    }

    waitForSystemCallCompletion(process);

    // Calcular tiempos
    int startTime = syncController.getScheduler().getCurrentTime();
    int endTime = startTime + duration;

    Logger.procLog(String.format("[T=%d] [I/O] Procesando I/O para %s (duración: %d, fin: t=%d)", 
      startTime, process.getPid(), duration, endTime));

    // Esperar hasta el tiempo de finalización
    waitUntilIOCompletes(process, endTime);

    // Completar la operación I/O
    completeIOOperation(process, ioBurst, duration);
  }

  private void waitUntilIOCompletes(Process process, int endTime) throws InterruptedException {
    while(isRunning() && process.getState() != ProcessState.TERMINATED) {
      int currentSimTime = syncController.getScheduler().getCurrentTime();
      
      // Verificar si ya completó
      if (currentSimTime >= endTime) {
        break;
      }
      
      synchronized(ioMonitor) {
        // Doble verificación dentro del lock
        if (isRunning() && 
            process.getState() != ProcessState.TERMINATED && 
            syncController.getScheduler().getCurrentTime() < endTime) {
          ioMonitor.wait(50); // Polling cada 50ms
        } else {
          break;
        }
      }
    }
  }

  private void completeIOOperation(Process process, Burst ioBurst, int duration) {
    // Verificar que el proceso siga activo
    if (!isRunning() || process.getState() == ProcessState.TERMINATED) {
      return;
    }
    
    // Ejecutar burst (marca como completado)
    ioBurst.execute(duration);
    
    // Avanzar a siguiente ráfaga
    process.advanceBurst();
    
    // Actualizar estadísticas (thread-safe con AtomicInteger)
    completedIOOperations.incrementAndGet();
    totalIOTime.addAndGet(duration);
    
    // Obtener tiempo actual
    int completionTime = syncController.getScheduler().getCurrentTime();
    
    Logger.procLog(String.format("[T=%d] [I/O] I/O completada para %s (duración: %d unidades)", 
      completionTime, process.getPid(), duration));
    
    syncController.notifyProcessReady(process, "completó I/O");
  }

  private void waitForSystemCallCompletion(Process process) throws InterruptedException {
    if (!process.isInSystemCall()) {
        return;  // No hay system call pendiente
    }
    
    int endTime = process.getSystemCallEndTime();
    
    while(isRunning() && process.getState() != ProcessState.TERMINATED) {
      int currentSimTime = syncController.getScheduler().getCurrentTime();
      
      // Verificar si ya completó el system call
      if (currentSimTime >= endTime) {
        Logger.procLog(String.format("[T=%d] [SYSTEM CALL] %s completó system call", 
            currentSimTime, process.getPid()));
        process.clearSystemCall();
        break;
      }
      
      synchronized(ioMonitor) {
        if (isRunning() && 
            process.getState() != ProcessState.TERMINATED && 
            syncController.getScheduler().getCurrentTime() < endTime) {
          ioMonitor.wait(50);
        } else {
          break;
        }
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
    // AtomicInteger.get() es thread-safe, no necesita locks
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