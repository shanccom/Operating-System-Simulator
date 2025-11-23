package modules.sync;

import model.Process;
import model.ProcessState;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import utils.Logger;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Coordina sincronizacion entre planificador y memoria
public class SyncController {
    
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    
    // Locks para sincronizacion
    private final Lock schedulerLock;
    private final Lock memoryLock;
    private final Lock globalLock;
    
    // Conditions para esperas coordinadas
    private final Condition memoryAvailable;
    private final Condition processReady;
    
    // Estado del controlador
    private volatile boolean running;
    private volatile boolean paused;
    
    public SyncController(Scheduler scheduler, MemoryManager memoryManager) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
        
        this.schedulerLock = new ReentrantLock();
        this.memoryLock = new ReentrantLock();
        this.globalLock = new ReentrantLock();
        
        this.memoryAvailable = globalLock.newCondition();
        this.processReady = globalLock.newCondition();
        
        this.running = false;
        this.paused = false;
        
        Logger.log("SyncController inicializado");
    }
    
    // Prepara un proceso para ejecucion
    public boolean prepareProcessForExecution(Process process) {
        globalLock.lock();
        try {
            Logger.debug("Preparando proceso " + process.getPid() + " para ejecucion");
            
            // Verificar que el proceso este en estado valido
            if (process.getState() != ProcessState.READY) {
                Logger.warning("Proceso " + process.getPid() + " no esta en estado READY");
                return false;
            }
            
            // Cargar las paginas necesarias
            boolean pagesLoaded = loadRequiredPages(process);
            
            if (!pagesLoaded) {
                // Bloquear el proceso por memoria
                process.setState(ProcessState.BLOCKED_MEMORY);
                Logger.log("Proceso " + process.getPid() + " bloqueado por falta de memoria");
                return false;
            }
            
            // Proceso listo para ejecutar
            process.setState(ProcessState.RUNNING);
            return true;
            
        } finally {
            globalLock.unlock();
        }
    }
    
    // Carga las paginas requeridas por un proceso
    private boolean loadRequiredPages(Process process) {
        memoryLock.lock();
        try {
            int requiredPages = process.getRequiredPages();
            
            // Para simplificar: intentamos cargar todas las paginas del proceso
            // En una implementacion mas realista, solo cargariamos las necesarias para la rafaga actual
            
            for (int page = 0; page < requiredPages; page++) {
                if (!memoryManager.isPageLoaded(process.getPid(), page)) {
                    boolean loaded = memoryManager.loadPage(process, page);
                    if (!loaded) {
                        Logger.warning("No se pudo cargar pagina " + page + 
                                     " del proceso " + process.getPid());
                    }
                }
            }
            
            // Verificar que todas las paginas necesarias esten cargadas
            int loadedCount = memoryManager.getLoadedPages(process.getPid()).size();
            
            if (loadedCount >= Math.min(requiredPages, memoryManager.getTotalFrames())) {
                Logger.debug("Todas las paginas necesarias de " + process.getPid() + " estan cargadas");
                return true;
            }
            
            return false;
            
        } finally {
            memoryLock.unlock();
        }
    }
    
    // Notifica que un proceso ha sido bloqueado
    public void notifyProcessBlocked(Process process, ProcessState blockReason) {
        globalLock.lock();
        try {
            Logger.logStateChange(process.getPid(), process.getState(), blockReason, 
                                scheduler.getCurrentTime());
            process.setState(blockReason);
            
        } finally {
            globalLock.unlock();
        }
    }
    
    // Notifica que un proceso esta listo nuevamente
    public void notifyProcessReady(Process process) {
        globalLock.lock();
        try {
            if (process.getState().isBlocked()) {
                Logger.logStateChange(process.getPid(), process.getState(), 
                                    ProcessState.READY, scheduler.getCurrentTime());
                scheduler.addProcess(process);
                processReady.signalAll();
            }
        } finally {
            globalLock.unlock();
        }
    }
    
    // Espera hasta que haya un proceso listo
    public void waitForReadyProcess() throws InterruptedException {
        globalLock.lock();
        try {
            while (running && !scheduler.hasReadyProcesses()) {
                processReady.await();
            }
        } finally {
            globalLock.unlock();
        }
    }
    
    // Espera hasta que haya memoria disponible
    public void waitForMemoryAvailable() throws InterruptedException {
        globalLock.lock();
        try {
            while (running && memoryManager.getFreeFrames() == 0) {
                memoryAvailable.await();
            }
        } finally {
            globalLock.unlock();
        }
    }
    
    // Notifica que hay memoria disponible
    public void notifyMemoryAvailable() {
        globalLock.lock();
        try {
            memoryAvailable.signalAll();
        } finally {
            globalLock.unlock();
        }
    }
    
    // Sincroniza el tiempo entre scheduler y memoria
    public void synchronizeTime(int time) {
        globalLock.lock();
        try {
            scheduler.setCurrentTime(time);
            memoryManager.setCurrentTime(time);
        } finally {
            globalLock.unlock();
        }
    }
    
    // Libera los recursos de un proceso terminado
    public void releaseProcessResources(Process process) {
        globalLock.lock();
        try {
            // Liberar paginas de memoria
            memoryLock.lock();
            try {
                memoryManager.freeProcessPages(process.getPid());
                memoryAvailable.signalAll();
            } finally {
                memoryLock.unlock();
            }
            
            // Actualizar estado
            process.setState(ProcessState.TERMINATED);
            scheduler.onProcessComplete(process);
            
            Logger.log("Recursos del proceso " + process.getPid() + " liberados");
            
        } finally {
            globalLock.unlock();
        }
    }
    
    // Inicia el controlador
    public void start() {
        globalLock.lock();
        try {
            running = true;
            Logger.log("SyncController iniciado");
        } finally {
            globalLock.unlock();
        }
    }
    
    // Detiene el controlador
    public void stop() {
        globalLock.lock();
        try {
            running = false;
            processReady.signalAll();
            memoryAvailable.signalAll();
            Logger.log("SyncController detenido");
        } finally {
            globalLock.unlock();
        }
    }
    
    // Pausa la simulacion
    public void pause() {
        globalLock.lock();
        try {
            paused = true;
            Logger.log("Simulacion pausada");
        } finally {
            globalLock.unlock();
        }
    }
    
    // Reanuda la simulacion
    public void resume() {
        globalLock.lock();
        try {
            paused = false;
            processReady.signalAll();
            Logger.log("Simulacion reanudada");
        } finally {
            globalLock.unlock();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
}