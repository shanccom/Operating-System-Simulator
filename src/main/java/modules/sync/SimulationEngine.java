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
import model.Burst;

public class SimulationEngine {

  private final Scheduler scheduler;
  private final MemoryManager memoryManager;
  private final SyncController syncController;
  private final IOManager ioManager;

  private final List<Process> allProcesses;
  private final List<ProcessThread> processThreads;

  private final Config config;
  // Objeto que contendra los resultados finales
  private DatosResultados datosFinales;

  private final Object engineMonitor = new Object();

  private int currentTime;
  private volatile boolean running;

  private SimulationStateListener stateListener;
  // para gant chart
  private final Map<String, Integer> executionStartTimes = new HashMap<>();

  // paso a paso
  private SimulationController simulationController;

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

    this.simulationController = new SimulationController();
    memoryManager.setSimulationController(this.simulationController); // COnecto el Simulation Controller del memrmoy
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
    ioManager.setStateListener(listener);

  }

  // Notificar cambios a la UI
  private void notifyUIUpdate() {
    if (stateListener != null) {
      List<Process> readyQueue = scheduler.getReadyQueueSnapshot();

      List<Process> blockedIO;
      List<Process> blockedMemory;

      synchronized (engineMonitor) {
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
          p.getPageFaults()));
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
        memoryManager.getAlgorithmName());
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

      try {
        simulationController.waitForNextStep();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }

      synchronized (syncController.getCoordinationMonitor()) {
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
        if (currentTime > endTime) {

          Burst nextBurst = p.getCurrentBurst();
          
          if (nextBurst != null && !nextBurst.isCPU()) {
            
            // Siguiente es I/O → cambiar a RUNNING para que ejecute I/O
            Logger.exeLog(String.format("[T=%d] [ENGINE] %s CONTEXT_SWITCHING → RUNNING (ejecutará I/O)", 
              currentTime, p.getPid()));
            
            p.setState(ProcessState.RUNNING);
            p.clearContextSwitch();
            thread.wakeUp(); // Despertar para que ejecute I/O
            
          } else {


            // Siguiente es CPU → volver a READY
            Logger.exeLog(String.format("[T=%d] [ENGINE] %s CONTEXT_SWITCHING → READY", 
              currentTime, p.getPid()));
            
            p.setState(ProcessState.READY);
            scheduler.addProcess(p);
            p.clearContextSwitch();
          }
        } else {
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
    synchronized (engineMonitor) {
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

    System.out.println(">>> [DEBUG-VALIDATE] T=" + currentTime + 
                       " validando " + current.getPid() + 
                       " estado=" + state);


    // Verificar estado válido
    if (state == ProcessState.TERMINATED ||
        state == ProcessState.BLOCKED_IO ||
        state == ProcessState.BLOCKED_MEMORY) {

      System.out.println(">>> [DEBUG-VALIDATE] " + current.getPid() + 
      " no puede continuar (estado=" + state + ")");
      
      notifyProcessExecutionEnded(current, currentTime, "bloqueado/terminado");

      scheduler.setCurrentProcess(null);
      selectNextProcess();
      return;
    }

    // Verificar memoria durante ejecución
    if (!syncController.hasAllRequiredPages(current)) {
      Logger.syncLog("[ENGINE] " + current.getPid() + " perdió páginas");
      current.setState(ProcessState.READY);
      scheduler.addProcess(current);
      
      notifyProcessExecutionEnded(current, currentTime, "perdió páginas");
      
      scheduler.setCurrentProcess(null);
      selectNextProcess();
      return;
    }

    System.out.println(">>> [DEBUG-VALIDATE] " + current.getPid() + 
                       " sigue siendo RUNNING, checando quantum...");

    // Manejar expropiación por quantum (Round Robin)
    handleQuantumExpropriation(current);

    // Si sigue siendo RUNNING, permite continuar
    if (current.getState() == ProcessState.RUNNING) {

      System.out.println(">>> [DEBUG-VALIDATE] " + current.getPid() + 
                         " continúa en RUNNING");

      wakeUpProcessThread(current);
      scheduler.recordCPUTime(1);
    } else {
      System.out.println(">>> [DEBUG-VALIDATE] " + current.getPid() + 
                         " cambió de estado (quantum agotado probablemente)");

      notifyProcessExecutionEnded(current, currentTime - 1 , "quantum agotado");

      selectNextProcess();
    }
  }

  private void handleQuantumExpropriation(Process current) {
    if (scheduler instanceof modules.scheduler.RoundRobin) {
      modules.scheduler.RoundRobin rr = (modules.scheduler.RoundRobin) scheduler;
      
      System.out.println(">>> [DEBUG-QUANTUM] T=" + currentTime + 
      " [" + current.getPid() + "] antes de decrementar");
      System.out.println(">>>   Quantum actual: " + rr.getQuantum());
      
      rr.decrementaQuantum();

      System.out.println(">>> [DEBUG-QUANTUM] [" + current.getPid() + 
                         "] después de decrementar");
      System.out.println(">>>   Quantum: " + rr.getQuantum() + 
                         " agotado=" + rr.isQuantumAgotado());



      if (rr.isQuantumAgotado()) {
        int currentTime = getCurrentTime();
        int overhead = config.getContextSwitchOverhead();
        int endTime = currentTime;

        System.out.println(">>> [DEBUG-QUANTUM] ⚠️ QUANTUM AGOTADO");
        System.out.println(">>>   T=" + currentTime + 
                           " → CONTEXT_SWITCHING hasta T=" + endTime);


        Logger.exeLog(String.format(
            "[T=%d] [ENGINE] Quantum agotado: %s  CONTEXT_SWITCHING (hasta t=%d)",
            currentTime, current.getPid(), endTime));

        current.setState(ProcessState.CONTEXT_SWITCHING);
        current.setContextSwitchEndTime(endTime);
        scheduler.setCurrentProcess(null);

        System.out.println(">>> [DEBUG-QUANTUM] Reseteando quantum para próximo turno");
        rr.resetQuantum();
      }
    }
  }

  private void selectNextProcess() {
    long inCS = allProcesses.stream().filter(p -> p.getState() == ProcessState.CONTEXT_SWITCHING).count();
    
    System.out.println("\n>>> [DEBUG-SELECT] T=" + currentTime + 
                       " - selectNextProcess");
    System.out.println(">>>   Procesos en CONTEXT_SWITCHING: " + inCS);
    
    if (inCS > 0) {
      System.out.println(">>> [DEBUG-SELECT] Hay procesos en context switch, esperando...");
      Logger.syncLog(String.format("[T=%d] [ENGINE] %d proceso(s) en CONTEXT_SWITCHING",
          getCurrentTime(), inCS));
      scheduler.recordIdleTime(1);
      return;
    }

    Process currentProcess = scheduler.getCurrentProcess();

    System.out.println(">>> [DEBUG-SELECT] Seleccionando siguiente proceso...");
    System.out.println(">>>   currentProcess (saliente): " + 
                       (currentProcess != null ? currentProcess.getPid() : "NULL"));

    Process nextProcess = scheduler.selectNextProcess();

    System.out.println(">>> [DEBUG-SELECT] nextProcess (entrante): " + 
                       (nextProcess != null ? nextProcess.getPid() : "NULL"));


    if (nextProcess == null) {
      System.out.println(">>> [DEBUG-SELECT] No hay próximo proceso, CPU ociosa");
      scheduler.recordIdleTime(1);
      return;
    }

    if (currentProcess != null && currentProcess != nextProcess) {
      int overhead = config.getContextSwitchOverhead();

      System.out.println(">>> [DEBUG-SELECT] CONTEXT SWITCH: " + 
                         currentProcess.getPid() + " → " + nextProcess.getPid() + 
                         " (overhead=" + overhead + ")");

      int currentTime = getCurrentTime();

      Logger.exeLog(String.format("[T=%d] [ENGINE] Context Switch: %s  %s (overhead: %d ciclos)",
          currentTime, currentProcess.getPid(), nextProcess.getPid(), overhead));

      scheduler.recordIdleTime(overhead);
    }

    boolean canExecute = syncController.prepareProcessForExecution(nextProcess);

    if (canExecute) {
      
      //para gant
      String pid = nextProcess.getPid();

      System.out.println(">>> [DEBUG-SELECT] ✅ " + pid + 
                         " INICIANDO EJECUCIÓN en T=" + currentTime);

      if (stateListener != null) {
        stateListener.onProcessExecutionStarted(pid, currentTime);
        // Solo notificar context switch si hubo un proceso anterior
        if (currentProcess != null) {
          stateListener.onContextSwitch();
        }
      }
      executionStartTimes.put(pid, currentTime);
      // fin

      scheduler.confirmProcessSelection(nextProcess);
      wakeUpProcessThread(nextProcess);
      scheduler.recordCPUTime(1);

    } else {
      // Vuelve a la cola
      System.out.println(">>> [DEBUG-SELECT] ❌ " + nextProcess.getPid() + 
                         " no puede ejecutar aún (esperando recursos)");
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
    // para gant
    // notificar fin de ejecucio de procesos que terminaron
    synchronized (engineMonitor) {
      for (String pid : new ArrayList<>(executionStartTimes.keySet())) {
        Integer startTime = executionStartTimes.get(pid);
        if (startTime != null && stateListener != null) {
          stateListener.onProcessExecutionEnded(pid, currentTime);
        }
      }
      executionStartTimes.clear();
    }
    // fin

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
          p.getPageFaults()));
    }
  }

  public void stop() {
    synchronized (engineMonitor) {
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

  public SimulationController getSimulationController() {
    return simulationController;
  }

  public synchronized int getCurrentTime() {
    return currentTime;
  }

  public synchronized boolean isRunning() {
    return running;
  }

  public SyncController getSyncController() {
    return syncController;
  }

  private void notifyProcessExecutionEnded(Process process, int endTime, String reason) {
    if (stateListener == null) {
      return;
    }
    
    String pid = process.getPid();
    Integer startTime = executionStartTimes.get(pid);
    
    if (startTime != null) {
      System.out.println(">>> [DEBUG-GANTT] onProcessExecutionEnded(" + pid + ", " + 
                         endTime + ") - razón: " + reason);
      System.out.println(">>>   Duración: " + startTime + " a " + endTime + 
                         " (total: " + (endTime - startTime) + " ciclos)");
      
      stateListener.onProcessExecutionEnded(pid, endTime);
      executionStartTimes.remove(pid);
    }
  }


}