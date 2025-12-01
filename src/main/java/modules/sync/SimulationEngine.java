package modules.sync;

import model.Config;
import model.Process;
import model.ResultadoProceso;
import model.DatosResultados;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
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


  private synchronized void startAllThreads() {
    for (ProcessThread thread : processThreads) {
      thread.start();
      Logger.syncLog("  Thread creado: " + thread.getName());
      sleep(10);
    }
  }

  private void coordinationLoop() { 
    while (isRunning() && !allProcessesCompleted()) {
      synchronized(syncController.getCoordinationMonitor()) {
        int t = getCurrentTime();
        
        // Actualizar tiempo en SyncController
        syncController.synchronizeTime(t);
        
        // Notificar procesos en tiempo de llegada
        notifyProcessArrivals(t);

        handleContextSwitchCompletion(t);

        countContextSwitchingCycles();
        
        // Coordinar ejecución
        coordinateScheduler();
        
        // Actualizar tiempos de espera
        updateWaitingTimes();
        
        // Revisar procesos bloqueados
        notifyBlockedProcesses();
        
        // Notificar UI
        notifyUIUpdate();
      }
      
      sleep(config.getTimeUnit());
      
      // Avanzar tiempo fuera del lock extendido
      advanceTime();
    }
  }

  private void handleContextSwitchCompletion(int currentTime) {
    for (ProcessThread thread : processThreads) {
      Process p = thread.getProcess();
      
      // Verificar si está en CONTEXT_SWITCHING
      if (p.getState() == ProcessState.CONTEXT_SWITCHING) {
        int endTime = p.getContextSwitchEndTime();
        
        // Si el tiempo de overhead ya pasó
        if (currentTime >= endTime) {
          Logger.exeLog(String.format("[T=%d] [ENGINE] %s CONTEXT_SWITCHING a READY", 
            currentTime, p.getPid()));
          
          // Volver a READY
          p.setState(ProcessState.READY);
          scheduler.addProcess(p);
          p.clearContextSwitch();
        }
      }
    }
  }

  private void countContextSwitchingCycles() {
    long contextswitchingCount = allProcesses.stream()
      .filter(p -> p.getState() == ProcessState.CONTEXT_SWITCHING)
      .count();
    
    if (contextswitchingCount > 0) {
      scheduler.recordIdleTime((int) contextswitchingCount);
      Logger.exeLog(String.format("[T=%d] [ENGINE] %d proceso(s) en CONTEXT_SWITCHING (CPU ociosa)", 
        getCurrentTime(), contextswitchingCount));
    }
  }

  private void notifyProcessArrivals(int t) {
    // Ya estamos dentro de coordinationMonitor en coordinationLoop()
    
    // Despertar todos los threads para que verifiquen si llegaron
    for (ProcessThread thread : processThreads) {
      Process process = thread.getProcess();
      int arrivalTime = process.getArrivalTime();
      
      // Si es el momento exacto de llegada o ya pasó
      if (t == arrivalTime && process.getState() == ProcessState.NEW) {
        Logger.syncLog(String.format("[T=%d] [ARRIVAL] %s llega al sistema", 
          t, process.getPid()));
        // El thread se encargará de cambiar a READY
        thread.wakeUp();
      }
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
    
    for (ProcessThread thread : processThreads) {
      Process p = thread.getProcess();
      ProcessState state = p.getState();
      

      if (state == ProcessState.CONTEXT_SWITCHING) {
        continue;
      }

      if (state == ProcessState.BLOCKED_IO) {
        // IOManager se encargará de desbloquear
        // Solo nos aseguramos que siga bloqueado
        
      } else if (state == ProcessState.BLOCKED_MEMORY) {
        if (p.isWaitingForPageFault()) {
          int endTime = p.getPageFaultEndTime();
          if (currentTime >= endTime) {
            Logger.memLog(String.format("[T=%d] [PAGE FAULT] %s completó penalty", 
              currentTime, p.getPid()));
            p.clearPageFault();
            thread.wakeUp();
          }
        }
      }
    }
  }

  private synchronized void advanceTime() {
    currentTime++;
    scheduler.setCurrentTime(currentTime);
  }
  
  private void coordinateScheduler() {
    Process currentProcess = scheduler.getCurrentProcess();
    
    if (currentProcess != null) {
      validateAndContinueProcess(currentProcess);
    } else {
      selectNextProcess();
    }
  }

  private void validateAndContinueProcess(Process current) {
    ProcessState state = current.getState();
    
    // Verificar estado válido
    if (state == ProcessState.TERMINATED || 
        state == ProcessState.BLOCKED_IO || 
        state == ProcessState.BLOCKED_MEMORY) {
      scheduler.setCurrentProcess(null);
      selectNextProcess();
      return;
    }
    
    // Verificar memoria durante ejecución
    if (!syncController.hasAllRequiredPages(current)) {
      Logger.syncLog("[ENGINE] " + current.getPid() + " perdió páginas");
      current.setState(ProcessState.READY);
      scheduler.addProcess(current);
      scheduler.setCurrentProcess(null);
      selectNextProcess();
      return;
    }
    
    // Manejar expropiación por quantum (Round Robin)
    handleQuantumExpropriation(current);
    
    // Si sigue siendo RUNNING, permite continuar
    if (current.getState() == ProcessState.RUNNING) {
      wakeUpProcessThread(current);
      scheduler.recordCPUTime(1);
    } else {
      selectNextProcess();
    }
  }

  private void handleQuantumExpropriation(Process current) {
    if (scheduler instanceof modules.scheduler.RoundRobin) {
      modules.scheduler.RoundRobin rr = (modules.scheduler.RoundRobin) scheduler;
      rr.decrementaQuantum();
      
      if (rr.isQuantumAgotado()) {
        int currentTime = getCurrentTime();
        int overhead = config.getContextSwitchOverhead();
        int endTime = currentTime + overhead;
        
        Logger.exeLog(String.format(
          "[T=%d] [ENGINE] Quantum agotado: %s  CONTEXT_SWITCHING (hasta t=%d)", 
          currentTime, current.getPid(), endTime));
        
        current.setState(ProcessState.CONTEXT_SWITCHING);
        current.setContextSwitchEndTime(endTime);
        scheduler.setCurrentProcess(null);
      }
    }
  }

  private void selectNextProcess() {
    long inCS = allProcesses.stream().filter(p -> p.getState() == ProcessState.CONTEXT_SWITCHING).count();
    if (inCS > 0) {
      Logger.syncLog(String.format("[T=%d] [ENGINE] %d proceso(s) en CONTEXT_SWITCHING", 
        getCurrentTime(), inCS));
      scheduler.recordIdleTime(1);
      return;
    }
    
    Process currentProcess = scheduler.getCurrentProcess();
    Process nextProcess = scheduler.selectNextProcess();
    
    if (nextProcess == null) {
      scheduler.recordIdleTime(1);
      return;
    }
    
    if (currentProcess != null && currentProcess != nextProcess) {
      int overhead = config.getContextSwitchOverhead();
      int currentTime = getCurrentTime();
      
      Logger.exeLog(String.format("[T=%d] [ENGINE] Context Switch: %s  %s (overhead: %d ciclos)", 
        currentTime, currentProcess.getPid(), nextProcess.getPid(), overhead));
      
      scheduler.recordIdleTime(overhead);
    }

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
      
      scheduler.confirmProcessSelection(nextProcess);
      wakeUpProcessThread(nextProcess);
      scheduler.recordCPUTime(1);

    } else {
      // Vuelve a la cola
      scheduler.recordIdleTime(1);
    }
  }

  private void wakeUpProcessThread(Process process) {
    for (ProcessThread thread : processThreads) {
      if (thread.getProcess() == process) {
        thread.wakeUp();
        break;
      }
    }
  }

  private synchronized boolean allProcessesCompleted() {
    return allProcesses.stream()
      .allMatch(p -> p.getState() == ProcessState.TERMINATED);
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