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
  private final Config config;

  // Monitor local para sincronización interna del thread
  private final Object threadMonitor = new Object();
  private volatile boolean running;
  
  //Listener y mapa de tiempos
  private SimulationStateListener stateListener;
  private Map<String, Integer> executionStartTimes;

  public ProcessThread(Process process, SyncController syncController, 
                       IOManager ioManager, Config config) {
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
    } finally {
      synchronized(syncController.getCoordinationMonitor()) {
        if (process.getState() != ProcessState.TERMINATED) {
          terminateProcess();
        }
      }
    }
  }

  private void waitForArrival() throws InterruptedException {
    int arrivalTime = process.getArrivalTime();
    
    while(running) {
      synchronized(syncController.getCoordinationMonitor()) {
        int currentTime = syncController.getCurrentTime();
        
        if (currentTime >= arrivalTime) {
          break;
        }
      }
      
      synchronized(threadMonitor) {
        threadMonitor.wait(50); // Polling cada 50ms es aceptable en simulación
      }
    }
    
    Logger.procLog(String.format("[T=%d] [%s] Proceso llegó al sistema", 
      arrivalTime, process.getPid()));
  }

  private void arriveAtSystem() {
    synchronized(syncController.getCoordinationMonitor()) {
      if (process.getState() != ProcessState.TERMINATED) {
        process.setState(ProcessState.READY);
        syncController.notifyProcessReady(process, "llegada al sistema");
      }
    }
  }

  private void mainExecutionLoop() throws InterruptedException {
    while (running) {
      synchronized(syncController.getCoordinationMonitor()) {
        if (process.isCompleted() || process.getState() == ProcessState.TERMINATED) {
          break;
        }
      }
      
      waitForRunningState();
      
      synchronized(syncController.getCoordinationMonitor()) {
        ProcessState state = process.getState();
        if (!running || state == ProcessState.TERMINATED) {
          break;
        }
        
        if (state == ProcessState.CONTEXT_SWITCHING) {
          continue;
        }

        Burst currentBurst = process.getCurrentBurst();

      if (currentBurst != null && !currentBurst.isCPU() && 
          state == ProcessState.RUNNING) {
        
        executeIOBurst(currentBurst);
        handleBurstCompletion(currentBurst);
        continue; // Volver a esperar el siguiente
      }

        if (state != ProcessState.RUNNING) {
          continue; // Volver a esperar
        }
      }
      
      // Obtener ráfaga actual
      Burst currentBurst = process.getCurrentBurst();
      if (currentBurst == null) {
        break;
      }

      // Ejecutar según tipo
      if (currentBurst.isCPU()) {
        executeCPUBurst(currentBurst);
        syncController.getMemoryManager().updateProcessPagesAccessTime(process.getPid()); // Actualizar acceso a páginas CUIDADO
      } else {
        executeIOBurst(currentBurst);
        syncController.getMemoryManager().updateProcessPagesAccessTime(process.getPid()); // Actualizar acceso a páginas CUIDADO
      }
      
      handleBurstCompletion(currentBurst);

    }
    
    terminateProcess();
  }

  private void waitForRunningState() throws InterruptedException {
    synchronized(threadMonitor) {
      while(running) {
        ProcessState state;
        synchronized(syncController.getCoordinationMonitor()) {
          state = process.getState();
          
          Burst currentBurst = process.getCurrentBurst();
          if (currentBurst != null && !currentBurst.isCPU() && 
              state != ProcessState.CONTEXT_SWITCHING && 
              state != ProcessState.BLOCKED_IO) {
            break; // Puede ejecutar I/O
          }
        
        }
        
        if (state == ProcessState.RUNNING || state == ProcessState.TERMINATED) {
          break;
        }
        
        threadMonitor.wait(); // Esperar sin timeout
      }
    }
  }

  private void executeCPUBurst(Burst burst) throws InterruptedException {
    while (!burst.isCompleted() && running) {
      ProcessState state;
      synchronized(syncController.getCoordinationMonitor()) {
        state = process.getState();
        
        if (state != ProcessState.RUNNING) {
          break;
        }
        
        if (!syncController.hasAllRequiredPages(process)) {
          handleMemoryLack();
          return;
        }
      }
      
      // Ejecutar una unidad de CPU
      executeOneCPUUnit(burst);
      syncController.getMemoryManager().updateProcessPagesAccessTime(process.getPid()); // Actualizar acceso a páginas CUIDADO
      // CRÍTICO: Verificar si completó DENTRO del loop
      if (burst.isCompleted()) {
        int currentTime;
        synchronized(syncController.getCoordinationMonitor()) {
          currentTime = syncController.getCurrentTime();
        }


        Logger.exeLog(String.format("[T=%d] [%s] Rafaga CPU completada (%d unidades)", 
          currentTime, process.getPid(), burst.getDuration()));

        synchronized(syncController.getCoordinationMonitor()) {
          if (!process.isCompleted()) {
            // Notificar que este proceso liberó la CPU voluntariamente
            syncController.getScheduler().setCurrentProcess(null);
            Logger.exeLog(String.format("[T=%d] [%s] Libera CPU (ráfaga completada)", 
              currentTime, process.getPid()));
          }
        }

        break; // Salir del loop inmediatamente sin wait
      }
      
      // Solo esperar si la ráfaga NO se completó
      synchronized(threadMonitor) {
        threadMonitor.wait();
      }
    }
  }

  private void executeOneCPUUnit(Burst burst) {
    int currentTime;
    synchronized(syncController.getCoordinationMonitor()) {
      currentTime = syncController.getCurrentTime();
      process.markFirstExecution(currentTime);
    }

    burst.execute(1);
    
    int progress = burst.getDuration() - burst.getRemainingTime();
    int remaining = burst.getRemainingTime();
    
    Logger.exeLog(String.format("[T=%d] [%s] Ejecutando CPU: %d/%d unidades (restante: %d)", 
      currentTime, process.getPid(), progress, burst.getDuration(), remaining));
  }


  private void handleMemoryLack() {
    synchronized(syncController.getCoordinationMonitor()) {
      int currentTime = syncController.getCurrentTime();
      Logger.syncLog(String.format("[T=%d] [%s] Falta memoria BLOCKED_MEMORY", 
        currentTime, process.getPid()));
  
      //para gant
      notifyExecutionEnd("bloqueado memoria");
      //fin
  
      syncController.blockProcessForMemory(process);
    }
  }

  private void executeIOBurst(Burst burst) throws InterruptedException {
    int currentTime;
    synchronized(syncController.getCoordinationMonitor()) {
      currentTime = syncController.getCurrentTime();
      
      Logger.procLog(String.format("[T=%d] [%s] Solicita I/O (duración: %d unidades)", 
        currentTime, process.getPid(), burst.getDuration()));
      
    
      // INICIO para gant
      notifyExecutionEnd("bloqueado I/O");
      //FIN

      if (process.getState() != ProcessState.TERMINATED) {
        process.setState(ProcessState.BLOCKED_IO);
      }
    }

    // Notificar cambio de contexto
    syncController.triggerReschedule();
    
    ioManager.requestIO(process, burst);

  }

  private void handleBurstCompletion(Burst currentBurst) {
    synchronized(syncController.getCoordinationMonitor()) {
      if (!currentBurst.isCompleted()) {
        return;
      }

      
      ProcessState state = process.getState();
      if (state == ProcessState.TERMINATED) {
        return;
      }
      

      process.advanceBurst();
      Burst nextBurst = process.getCurrentBurst();


      if (nextBurst == null) {

        return;
      }

      if (nextBurst.isCPU()) {
        // Siguiente es CPU → volver a READY
        if (state != ProcessState.RUNNING) {
          syncController.notifyProcessReady(process, "siguiente ráfaga CPU");
        }
      } else {
        int currentTime = syncController.getCurrentTime();
        int overhead = config.getContextSwitchOverhead();
        int endTime = currentTime + overhead;
        
        
        Logger.exeLog(String.format("[T=%d] [%s] CPU→I/O: CONTEXT_SWITCHING hasta t=%d", 
          currentTime, process.getPid(), endTime));
        
        // Notificar fin de ejecución para Gantt
        notifyExecutionEnd("context switch CPU→I/O");
        
        process.setState(ProcessState.CONTEXT_SWITCHING);
        process.setContextSwitchEndTime(endTime);


        // El scheduler debe seleccionar otro proceso
        syncController.triggerReschedule();
      }

    }
  }


  private void terminateProcess() {
    synchronized(syncController.getCoordinationMonitor()) {
      int currentTime = syncController.getCurrentTime();

      // Cambiar estado localmente
      //para gant
      notifyExecutionEnd("terminado");
      //fin
      
      process.setCompletionTime(currentTime);
      process.setState(ProcessState.TERMINATED);
      
      syncController.releaseProcessResources(process);
    }
    
    //printFinalMetrics();
  }
  //Comentado porque ya esta visualmente
  private void printFinalMetrics() {
    System.out.println();
    Logger.syncLog("METRICAS FINALES: " + process.getPid() + " ===");
    Logger.syncLog("  Tiempo de espera:  " + process.getWaitingTime());
    Logger.syncLog("  Tiempo de retorno: " + process.getTurnaroundTime());
    Logger.syncLog("  Tiempo respuesta:  " + process.getResponseTime());
    Logger.syncLog("  Fallos de página:  " + process.getPageFaults());
    System.out.println();
  }

  // metodo para notificar fin de ejecución
  private void notifyExecutionEnd(String reason) {
    if (stateListener != null && executionStartTimes != null) {
      String pid = process.getPid();
      Integer startTime = executionStartTimes.get(pid);
      
      if (startTime != null) {
        int currentTime = syncController.getScheduler().getCurrentTime();
        stateListener.onProcessExecutionEnded(pid, currentTime);
        executionStartTimes.remove(pid);
      }
    }
  }


  public void wakeUp() {
    synchronized(threadMonitor) {
      threadMonitor.notifyAll();
    }
  }

  public void stopThread() {
    synchronized(threadMonitor) {
      running = false;
      threadMonitor.notifyAll();
    }
  }

  public Process getProcess() { return process; }
}
