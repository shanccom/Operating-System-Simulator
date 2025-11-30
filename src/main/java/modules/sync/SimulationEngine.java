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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  //para gant chart
  private final Map<String, Integer> executionStartTimes = new HashMap<>();


  public SimulationEngine(Scheduler scheduler, MemoryManager memoryManager, 
                          List<Process> processes, Config config) {
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.syncController = new SyncController(scheduler, memoryManager, config);
    this.ioManager = new IOManager(syncController, config);
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


  // Metodo para pasar el listener a los threads

  public void setStateListener(SimulationStateListener listener) {
    this.stateListener = listener;
    
    // Pasar el listener y el mapa de tiempos a cada thread
    for (ProcessThread thread : processThreads) {
        thread.setStateListener(listener, executionStartTimes);
    }
  }
  
  // Notificar cambios a la UI
  private void notifyUIUpdate() {
    if (stateListener != null) {
      List<Process> readyQueue = scheduler.getReadyQueueSnapshot();
      
      List<Process> blockedIO;
      List<Process> blockedMemory;
      
      synchronized(engineMonitor) {
        blockedIO = allProcesses.stream()
          .filter(p -> p.getState() == ProcessState.BLOCKED_IO)
          .toList();
        
        blockedMemory = allProcesses.stream()
          .filter(p -> p.getState() == ProcessState.BLOCKED_MEMORY)
          .toList();
      }
      
      stateListener.onReadyQueueChanged(readyQueue);
      stateListener.onBlockedIOChanged(blockedIO);
      stateListener.onBlockedMemoryChanged(blockedMemory);
      stateListener.onTimeChanged(currentTime);
    }
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
    datosFinales = construirResultados();
  }

  public DatosResultados getDatosFinales() {
    return datosFinales;
  }

  private DatosResultados construirResultados() {
    double esperaProm = allProcesses.stream().mapToDouble(Process::getWaitingTime).average().orElse(0);
    double retornoProm = allProcesses.stream().mapToDouble(Process::getTurnaroundTime).average().orElse(0);
    double respuestaProm = allProcesses.stream().mapToDouble(Process::getResponseTime).average().orElse(0);

    int completados = (int) allProcesses.stream().filter(p -> p.getState() == ProcessState.TERMINATED).count();
    double usoCpu = scheduler.getCPUTimePercent(); 

    List<ResultadoProceso> resumen = new ArrayList<>();
    for (Process p : allProcesses) {
        resumen.add(new ResultadoProceso(
                p.getPid(),
                p.getWaitingTime(),
                p.getTurnaroundTime(),
                p.getResponseTime(),
                p.getPageFaults()
        ));
    }

    return new DatosResultados(
            esperaProm,
            retornoProm,
            respuestaProm,
            usoCpu,
            completados,
            allProcesses.size(),
            scheduler.getCambiosContexto(),
            scheduler.getTiempoCpuTotal(),
            scheduler.getTiempoOcioso(),
            memoryManager.getTotalPageLoads(),
            memoryManager.getPageFaults(),
            memoryManager.getPageReplacements(),
            memoryManager.getTotalFrames(),
            memoryManager.getFreeFrames(),
            resumen,
            scheduler.getAlgorithmName(),
            memoryManager.getAlgorithmName()
    );
  }


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
        // Verificar si ya completó el page fault
        if (p.isWaitingForPageFault()) {
          int currentTime = getCurrentTime();
          int endTime = p.getPageFaultEndTime();
          
          if (currentTime >= endTime) {
            // Ya terminó el page fault penalty
            Logger.memLog(String.format("[T=%d] [PAGE FAULT] %s completó penalty, intentando cargar", 
                currentTime, p.getPid()));
            p.clearPageFault();
            thread.wakeUp();
          }
        } else {
          // No está esperando page fault, intentar cargar páginas
          thread.wakeUp();
        }
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
      //para gant
      String pid = nextProcess.getPid();
      System.out.println("[Engine-Gant] Proceso " + pid + " inicia ejecución en t=" + currentTime);
      
      if (stateListener != null) {
          stateListener.onProcessExecutionStarted(pid, currentTime);
          // Solo notificar context switch si hubo un proceso anterior
          if (previousProcess != null) {
              stateListener.onContextSwitch();
          }
      }
      executionStartTimes.put(pid, currentTime);
      //fin

      
      // Confirmar selección (remueve de cola, registra context switch)
      scheduler.confirmProcessSelection(nextProcess);
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


  private void waitForAllThreads() {

    Logger.syncLog("ESPERANDO FINALIZACION DE THREADS");

    //para gant
    // notificar fin de ejecucio de procesos que terminaron 
    synchronized(engineMonitor) {
      for (String pid : new ArrayList<>(executionStartTimes.keySet())) {
        Integer startTime = executionStartTimes.get(pid);
        if (startTime != null && stateListener != null) {
          System.out.println("[Engine-gant] 🏁 Proceso " + pid + " completado en t=" + currentTime);
          stateListener.onProcessExecutionEnded(pid, currentTime);
        }
      }
      executionStartTimes.clear();
    }

    //fin

    for (ProcessThread thread : processThreads) {
      try {
        thread.join();
        Logger.syncLog("  Thread finalizado: " + thread.getName());
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