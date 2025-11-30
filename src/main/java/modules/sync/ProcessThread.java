package modules.sync;

import java.util.Map;

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
  
  //Listener y mapa de tiempos
  private SimulationStateListener stateListener;
  private Map<String, Integer> executionStartTimes;



  public ProcessThread(Process process, SyncController syncController, IOManager ioManager, Config config) {
    super("Thread-" + process.getPid());
    this.process = process;
    this.syncController = syncController;
    this.ioManager = ioManager;
    this.running = true;
    this.config = config;
  }

  // metodo para establecer el listener
  public void setStateListener(SimulationStateListener listener, Map<String, Integer> startTimes) {
    this.stateListener = listener;
    this.executionStartTimes = startTimes;
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
    
    // Esperar hasta que llegue el tiempo de llegada
    // No mantener lock mientras se accede al scheduler para evitar deadlocks
    synchronized(threadMonitor) {
      while(running) {
        // Leer tiempo fuera del lock para evitar deadlocks
        int currentTime = syncController.getScheduler().getCurrentTime();
        if (currentTime >= arrivalTime) {
          break;
        }
        threadMonitor.wait(50);
      }
    }
    Logger.log(String.format("[T=%d] [%s]    Proceso llego al sistema", arrivalTime, process.getPid()));
  }

  private void arriveAtSystem() {
    // Cambiar estado del proceso
    // Luego notificar fuera del lock para evitar deadlocks con SyncController
    synchronized(threadMonitor) {
      if (process.getState() != ProcessState.TERMINATED) {
        process.setState(ProcessState.READY);
      }
    }
    
    // Notificar fuera del threadMonitor para evitar deadlocks
    // syncController usa coordinationMonitor, no threadMonitor
    if (process.getState() != ProcessState.TERMINATED) {
      syncController.notifyProcessReady(process, "llegada al sistema");
    }
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

      // Avanzar ráfaga y manejar transiciones de estado
      Burst nextBurst = null;
      synchronized(threadMonitor) {
        if (currentBurst.isCompleted() && process.getState() != ProcessState.TERMINATED) {
          process.advanceBurst();
          nextBurst = process.getCurrentBurst();
        }
      }
      
      // Notificar fuera del lock para evitar deadlocks
      if (nextBurst != null && process.getState() != ProcessState.TERMINATED) {
        if (nextBurst.isIO()) {
          // Si la siguiente es I/O, no necesitamos volver a READY
          // El executeIOBurst ya se encargará
        } else if (nextBurst.isCPU()) {
          // Si la siguiente es CPU, volver a READY y notificar
          if (process.getState() != ProcessState.RUNNING) {
            syncController.notifyProcessReady(process, "siguiente ráfaga CPU");
          }
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
    while (!burst.isCompleted() && process.getState() == ProcessState.RUNNING && running) {
        // Verificar páginas requeridas (fuera de locks para evitar deadlocks)
        if (!syncController.hasRequiredPages(process)) {
            int currentTime = syncController.getScheduler().getCurrentTime();
            Logger.log(String.format("[T=%d] [%s]   Falta memoria proceso será bloqueado por MEMORIA", 
                currentTime, process.getPid()));

            //para gant
            notifyExecutionEnd("bloqueado memoria");
            //fin

            // Bloquear explícitamente por memoria y forzar cambio de contexto
            syncController.blockProcessForMemory(process);
            // El scheduler ya no debe considerar este proceso como actual
            syncController.getScheduler().setCurrentProcess(null);
            return;
        }
        
        // Verificar estado antes de ejecutar
        if (process.getState() == ProcessState.RUNNING && !burst.isCompleted() && running) {
            int currentTime = syncController.getScheduler().getCurrentTime();
            burst.execute(1);
            // Usar directamente el tiempo actual del scheduler para el log,
            // así cada unidad de CPU se asocia a una única t.
            int remaining = burst.getRemainingTime();
            int progress = burst.getDuration() - remaining;
            Logger.log(String.format("[T=%d] [%s] Ejecutando CPU: %d/%d unidades completadas (restante: %d)", 
                currentTime, process.getPid(), progress, burst.getDuration(), remaining));
        }
        
        // Esperar siguiente ciclo si la ráfaga no está completa
        if (!burst.isCompleted() && process.getState() == ProcessState.RUNNING && running) {
            synchronized(threadMonitor) {
                // Verificar nuevamente dentro del lock
                if (process.getState() == ProcessState.RUNNING && !burst.isCompleted() && running) {
                    threadMonitor.wait();
                }
            }
        }
    }

    if (burst.isCompleted()) {
      int currentTime = syncController.getScheduler().getCurrentTime();
      Logger.log(String.format("[T=%d] [%s]   Rafaga CPU completada (%d unidades)", 
          currentTime, process.getPid(), burst.getDuration()));
    }
  }


  private void executeIOBurst(Burst burst) throws InterruptedException {
    int currentTime = syncController.getScheduler().getCurrentTime();
    Logger.log(String.format("[T=%d] [%s] → Solicita operacion I/O (duracion: %d unidades)", 
        currentTime, process.getPid(), burst.getDuration()));
    
    //para gant
    notifyExecutionEnd("bloqueado I/O");
    //fin

    // Cambiar estado a BLOCKED_IO
    synchronized(threadMonitor) {
      if (process.getState() != ProcessState.TERMINATED) {
        process.setState(ProcessState.BLOCKED_IO);
      }
    }
    
    // Actualizar scheduler y solicitar I/O fuera del lock para evitar deadlocks
    if (process.getState() == ProcessState.BLOCKED_IO) {
      syncController.getScheduler().setCurrentProcess(null);
      ioManager.requestIO(process, burst);
    }
  }

  private void terminateProcess() {
    // Obtener tiempo de finalización antes de cambiar estado
    int currentTime = syncController.getScheduler().getCurrentTime();

    //para gant
    notifyExecutionEnd("terminado");
    //fin

    // Cambiar estado a TERMINATED
    synchronized(threadMonitor) {
      process.setCompletionTime(currentTime);
      process.setState(ProcessState.TERMINATED);
      running = false;
    }
    
    // Liberar recursos fuera del lock para evitar deadlocks
    // syncController.releaseProcessResources usa coordinationMonitor
    syncController.releaseProcessResources(process);
    
    System.out.println();
    Logger.log("[THREAD-" + process.getPid() + "] METRICAS FINALES:");
    Logger.log("  Tiempo de espera: " + process.getWaitingTime());
    Logger.log("  Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.log("  Fallos de página: " + process.getPageFaults());
    System.out.println();
  }

  // metodo para notificar fin de ejecución
  private void notifyExecutionEnd(String reason) {
    if (stateListener != null && executionStartTimes != null) {
      String pid = process.getPid();
      Integer startTime = executionStartTimes.get(pid);
      
      if (startTime != null) {
        int currentTime = syncController.getScheduler().getCurrentTime();
        System.out.println("[ProcessThread-Gant]Proceso " + pid + " termina ejecución en t=" + currentTime + " (" + reason + ")");
        stateListener.onProcessExecutionEnded(pid, currentTime);
        executionStartTimes.remove(pid);
      }
    }
  }


  public void wakeUp() {
    synchronized(threadMonitor) {
      threadMonitor.notifyAll();  // Despierta el thread que está en wait()
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