// modules/sync/SimulationEngine.java
package modules.sync;

import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {
    
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    private final SyncController syncController;
    private final List<Process> allProcesses;
    private final List<ProcessThread> processThreads;
    private final Config config;
    
    private int currentTime;
    private boolean running;
    
    public SimulationEngine(Scheduler scheduler, MemoryManager memoryManager,
                           List<Process> processes, Config config) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
        this.syncController = new SyncController(scheduler, memoryManager);
        this.allProcesses = processes;
        this.config = config;
        this.currentTime = 0;
        this.running = false;
        
        // Crear ProcessThreads para cada proceso
        this.processThreads = new ArrayList<>();
        for (Process process : processes) {
            ProcessThread thread = new ProcessThread(process, syncController);
            processThreads.add(thread);
        }
        
        Logger.log("SimulationEngine creado con " + processes.size() + " procesos");
    }
    
    public void run() {
        Logger.section("INICIO DE SIMULACIÓN");
        Logger.log("Algoritmo planificación: " + scheduler.getAlgorithmName());
        Logger.log("Algoritmo memoria: " + memoryManager.getAlgorithmName());
        Logger.log("Total procesos: " + allProcesses.size());
        
        running = true;
        syncController.start();
        
        // PASO 1: Iniciar todos los ProcessThreads
        startAllThreads();
        
        // PASO 2: Bucle principal - Coordinar el sistema
        coordinationLoop();
        
        // PASO 3: Esperar a que todos terminen
        waitForAllThreads();
        
        syncController.stop();
        
        Logger.section("FIN DE SIMULACIÓN");
        showResults();
    }
    
    private void startAllThreads() {
        Logger.log("Iniciando " + processThreads.size() + " threads de procesos...");
        
        for (ProcessThread thread : processThreads) {
            thread.start();
            Logger.debug("Thread iniciado: " + thread.getName());
        }
        
        Logger.log("Todos los threads iniciados");
    }
    
    private void coordinationLoop() {
        Logger.log("Iniciando bucle de coordinación...");
        
        while (running && !allProcessesCompleted()) {
            
            // Mostrar estado cada cierto tiempo
            if (currentTime % 5 == 0) {
                Logger.log("\n========== TIEMPO " + currentTime + " ==========");
                printSystemState();
            }
            
            // Sincronizar tiempo en todos los módulos
            syncController.synchronizeTime(currentTime);
            
            // Scheduler selecciona siguiente proceso
            coordinateScheduler();
            
            // Avanzar tiempo
            currentTime++;
            scheduler.incrementTime();
            
            // Pequeña pausa para visualización
            sleep(config.getTimeUnit());
        }
        
        Logger.log("Bucle de coordinación terminado");
    }
    
    private void coordinateScheduler() {
        Process nextProcess = scheduler.selectNextProcess();
        
        if (nextProcess == null) {
            Logger.debug("CPU IDLE - No hay procesos listos");
            scheduler.recordIdleTime(1);
            return;
        }
        
        boolean canExecute = syncController.prepareProcessForExecution(nextProcess);
        
        if (canExecute) {
            Logger.debug("Proceso " + nextProcess.getPid() + " seleccionado y preparado");
            wakeUpThread(nextProcess);
            scheduler.recordCPUTime(1);
        } else {
            Logger.debug("Proceso " + nextProcess.getPid() + " bloqueado");
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
        Logger.log("Esperando a que todos los threads terminen...");
        
        for (ProcessThread thread : processThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Logger.error("Error esperando thread: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        
        Logger.log("Todos los threads han terminado");
    }
    
    private boolean allProcessesCompleted() {
        return allProcesses.stream()
            .allMatch(p -> p.getState() == model.ProcessState.TERMINATED);
    }
    
    private void printSystemState() {
        // Cola READY
        List<Process> readyQueue = scheduler.getReadyQueueSnapshot();
        Logger.log("Cola READY: " + readyQueue.size() + " procesos");
        for (Process p : readyQueue) {
            Logger.log("  • " + p.getPid() + " (Espera: " + p.getWaitingTime() + ")");
        }
        
        // Proceso ejecutando
        Process running = scheduler.getCurrentProcess();
        if (running != null) {
            model.Burst burst = running.getCurrentBurst();
            Logger.log("EJECUTANDO: " + running.getPid() + 
                      " - " + (burst != null ? burst.getType() : "NULL"));
        } else {
            Logger.log("EJECUTANDO: [CPU IDLE]");
        }
        
        // Procesos bloqueados
        long blocked = allProcesses.stream()
            .filter(p -> p.getState().isBlocked())
            .count();
        Logger.log("BLOQUEADOS: " + blocked + " procesos");
        
        // Estado de memoria
        Logger.log("MEMORIA: " + memoryManager.getFreeFrames() + 
                  "/" + memoryManager.getTotalFrames() + " marcos libres");
    }
    
    private void showResults() {
        Logger.separator();
        scheduler.printMetrics();
        memoryManager.printMetrics();
        
        Logger.separator();
        Logger.section("MÉTRICAS POR PROCESO");
        for (Process p : allProcesses) {
            Logger.log(String.format(
                "%s: Espera=%d, Retorno=%d, Respuesta=%d, PageFaults=%d",
                p.getPid(),
                p.getWaitingTime(),
                p.getTurnaroundTime(),
                p.getResponseTime(),
                p.getPageFaults()
            ));
        }
        Logger.separator();
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
    
    public void stop() {
        running = false;
        for (ProcessThread thread : processThreads) {
            thread.stopThread();
        }
    }
    
    // Getters
    public int getCurrentTime() { return currentTime; }
    public boolean isRunning() { return running; }
    public SyncController getSyncController() { return syncController; }
}