package modules.sync;

import model.Config;
import model.Process;
import model.ResultadoProceso;
import model.DatosResultados;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.scheduler.RoundRobin;
import utils.Logger;
import model.ProcessState;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {
  
  private final Scheduler scheduler;
  private final MemoryManager memoryManager;
  private final SyncController syncController;
  private final IOManager ioManager;
  
  private final List<Process> allProcesses;
  private final List<ProcessThread> processThreads;
  
  private final Config config;
  //Objeto que contendra los resultados finales
  private DatosResultados datosFinales;

  private final Object engineMonitor = new Object();
  
  private int currentTime;
  private volatile boolean running;
  
  private SimulationStateListener stateListener;


  public SimulationEngine(Scheduler scheduler, MemoryManager memoryManager, 
                          List<Process> processes, Config config) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.syncController = new SyncController(scheduler, memoryManager);
    this.ioManager = new IOManager(syncController);
    this.allProcesses = processes;
    this.config = config;
    this.currentTime = 0;
    this.running = false;
    
    // Crear un thread independiente por cada proceso
    this.processThreads = new ArrayList<>();
    for (Process process : processes) {
      ProcessThread thread = new ProcessThread(process, syncController, ioManager, config);
      processThreads.add(thread);
    }
  }


  public void setStateListener(SimulationStateListener listener) {
    this.stateListener = listener;
  }

  public void run() {
    running = true;

    syncController.start();
    ioManager.start();

    startAllThreads();

    coordinationLoop();

    waitForAllThreads();

    ioManager.stop();
    syncController.stop();
    showResults();
  }/* 

  public DatosResultados run() {
    
    running = true;
    syncController.start();
    ioManager.start();
    startAllThreads();
    coordinationLoop();
    waitForAllThreads();
    ioManager.stop();
    syncController.stop();
    showResults();
    datosFinales = construirResultados();
    return datosFinales;
  }
*/

  private void startAllThreads() {
    for (ProcessThread thread : processThreads) {
      thread.start();
      Logger.syncLog("  Thread creado: " + thread.getName());
      sleep(10); // Pequeña pausa para evitar condiciones de carrera al inicio
    }
  }

  private void coordinationLoop() { 
    
    while (isRunning() && !allProcessesCompleted()) {
      int t = getCurrentTime();
      
      syncController.synchronizeTime(t);
      
      checkProcessArrivals(t);
      
      coordinateScheduler();
      
      updateWaitingTimes();
      
      notifyBlockedProcesses();
      
      
      notifyUIUpdate();
      
      sleep(config.getTimeUnit());
      
      coordinateScheduler();

      advanceTime();
    }
    
  }

  private void checkProcessArrivals(int t) {
    // Los threads manejan su propia llegada, solo los notificamos
    for (ProcessThread thread : processThreads) {
      thread.wakeUp(); // El thread verifica si su tiempo de llegada coincide
    }
  }

  private void updateWaitingTimes() {
    synchronized(engineMonitor) {
      for (Process p : allProcesses) {
        if (p.getState() == ProcessState.READY) {
          p.incrementWaitingTime();
        }
      }
    }
  }


  private void notifyBlockedProcesses() {
    // Despertar procesos bloqueados en I/O
    for (ProcessThread thread : processThreads) {
      Process p = thread.getProcess();
      if (p.getState() == ProcessState.BLOCKED_IO) {
        synchronized(p) {
          p.notifyAll();
        }
      }
    }
    
    for (ProcessThread thread : processThreads) {
      Process p = thread.getProcess();
      if (p.getState() == ProcessState.BLOCKED_MEMORY) {
        thread.wakeUp(); // SyncController verificará si hay memoria disponible
      }
    }
  }

  private void advanceTime() {
    synchronized(engineMonitor) {
      currentTime++;
      scheduler.setCurrentTime(currentTime);
    }
  }

  private void coordinateScheduler() {
    Process currentRunning = scheduler.getCurrentProcess();
    
    // Validar que el proceso actual sigue siendo válido
    currentRunning = validateCurrentProcess(currentRunning);
    
    // Manejar quantum y expropiación del proceso actual
    boolean shouldContinue = handleCurrentProcessExecution(currentRunning);
    
    // Si el proceso actual continúa, no seleccionar uno nuevo
    if (shouldContinue) {
      return;
    }
    
    // Seleccionar y ejecutar siguiente proceso
    selectAndExecuteNextProcess(currentRunning);
  }

  private Process validateCurrentProcess(Process current) {
    if (current == null) {
      return null;
    }
    
    // Verificar estados inválidos
    ProcessState state = current.getState();
    if (state == ProcessState.TERMINATED || 
        state == ProcessState.BLOCKED_IO || 
        state == ProcessState.BLOCKED_MEMORY) {
      scheduler.setCurrentProcess(null);
      return null;
    }
    
    // Verificar memoria durante ejecución
    if (state == ProcessState.RUNNING && !syncController.hasAllRequiredPages(current)) {
      Logger.syncLog("[ENGINE] " + current.getPid() + " perdió páginas durante ejecución");
      current.setState(ProcessState.READY);
      scheduler.addProcess(current);
      scheduler.setCurrentProcess(null);
      return null;
    }
    
    return current;
  }

  private boolean handleCurrentProcessExecution(Process current) {
    if (current == null || current.getState() != ProcessState.RUNNING) {
      return false;
    }
    
    // Verificar expropiación por quantum (Round Robin)
    if (scheduler instanceof RoundRobin) {
      RoundRobin rr = (RoundRobin) scheduler;
      rr.decrementaQuantum();
      
      if (rr.isQuantumAgotado()) {
        Logger.exeLog("[ENGINE] Quantum agotado para " + current.getPid());
        preemptProcess(current);
        return false;
      }
    }
    
    // Verificar expropiación por prioridad
    if (shouldPreemptForPriority(current)) {
      return false;
    }
    
    // El proceso continúa ejecutando
    if (syncController.canProcessExecute(current)) {
      wakeUpThread(current);
      scheduler.recordCPUTime(1);
      return true;
    } else {
      // Perdió memoria durante ejecución
      scheduler.setCurrentProcess(null);
      scheduler.recordIdleTime(1);
      return false;
    }
  }

  private boolean shouldPreemptForPriority(Process current) {
    List<Process> readyQueue = scheduler.getReadyQueueSnapshot();
    
    for (Process candidate : readyQueue) {
      if (scheduler.shouldPreempt(current, candidate)) {
        Logger.exeLog("[ENGINE] Expropiando " + current.getPid() + " por " + candidate.getPid());
        preemptProcess(current);
        return true;
      }
    }
    
    return false;
  }

  private void preemptProcess(Process process) {
    synchronized(syncController.getCoordinationMonitor()) {
      process.setState(ProcessState.READY);
    }
    scheduler.addProcess(process);
    scheduler.setCurrentProcess(null);
  }

  private void selectAndExecuteNextProcess(Process previousProcess) {
    // Si hay proceso previo en RUNNING, regresarlo a READY
    if (previousProcess != null && previousProcess.getState() == ProcessState.RUNNING) {
      preemptProcess(previousProcess);
    }
    
    // Seleccionar siguiente proceso
    Process nextProcess = scheduler.selectNextProcess();
    
    if (nextProcess == null) {
      // CPU idle
      scheduler.recordIdleTime(1);
      return;
    }
    
    // Preparar proceso (verificar y cargar memoria)
    boolean canExecute = syncController.prepareProcessForExecution(nextProcess);
    
    if (canExecute) {
      // Confirmar selección (remueve de cola, registra context switch)
      scheduler.confirmProcessSelection(nextProcess);
      wakeUpThread(nextProcess);
      scheduler.recordCPUTime(1);
    } else {
      // No puede ejecutar (falta memoria), queda en READY
      scheduler.recordIdleTime(1);
    }
  }


  private void wakeUpThread(Process process) {
    for (ProcessThread thread : processThreads) {
      if (thread.getProcess() == process) {
        thread.wakeUp();
        break;
      }
    }
  }


  private void notifyUIUpdate() {
    if (stateListener == null) {
      return;
    }
    
    List<Process> readyQueue = scheduler.getReadyQueueSnapshot();
    
    List<Process> blockedIO = allProcesses.stream()
      .filter(p -> p.getState() == ProcessState.BLOCKED_IO)
      .toList();
    
    List<Process> blockedMemory = allProcesses.stream()
      .filter(p -> p.getState() == ProcessState.BLOCKED_MEMORY)
      .toList();
    
    stateListener.onReadyQueueChanged(readyQueue);
    stateListener.onBlockedIOChanged(blockedIO);
    stateListener.onBlockedMemoryChanged(blockedMemory);
    stateListener.onTimeChanged(currentTime);
  }


  private void waitForAllThreads() {
    Logger.syncLog("ESPERANDO FINALIZACION DE THREADS");
    for (ProcessThread thread : processThreads) {
      try {
        thread.join();
        Logger.syncLog("  ✓ Thread finalizado: " + thread.getName());
      } catch (InterruptedException e) {
        Logger.error("Error esperando thread: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
  }


  private boolean allProcessesCompleted() {
    synchronized(engineMonitor) {
      return allProcesses.stream()
        .allMatch(p -> p.getState() == ProcessState.TERMINATED);
    }
  }


  private void showResults() {
    scheduler.printMetrics();
    memoryManager.printMetrics();
    
    Logger.syncLog("\nMETRICAS POR PROCESO");
    for (Process p : allProcesses) {
      Logger.syncLog(String.format(
        "%s: Espera=%d, Retorno=%d, Respuesta=%d, PageFaults=%d",
        p.getPid(),
        p.getWaitingTime(),
        p.getTurnaroundTime(),
        p.getResponseTime(),
        p.getPageFaults()
      ));
    }
  }


  public void stop() {
    synchronized(engineMonitor) {
      running = false;
    }
    
    for (ProcessThread thread : processThreads) {
      thread.stopThread();
    }
  }

  private void sleep(int ms) {
    if (ms > 0) {
      try {
        Thread.sleep(ms);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public synchronized int getCurrentTime() { return currentTime; }
  public synchronized boolean isRunning() { return running; }
  public SyncController getSyncController() { return syncController; }
}