package modules.scheduler;

import model.Process;
import utils.Logger;

/** Round Robin
 * Algoritmo apropiativo que asigna un quantum de tiempo a cada proceso
 */
public class RoundRobin extends Scheduler {
    
    private final int quantum;
    private int currentQuantumRemaining;
    
    public RoundRobin(int quantum) {
        super();
        
        if (quantum <= 0) {
            throw new IllegalArgumentException("El quantum debe ser mayor a 0");
        }
        
        this.quantum = quantum;
        this.currentQuantumRemaining = 0;
        
        Logger.exeLog("Planificador Round Robin inicializado (quantum=" + quantum + ")");
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        Process next = readyQueue.peek();
        
        if (next != null) {
            // Resetear quantum completo al nuevo proceso (se asignará en confirmProcessSelection)
            Logger.procLog(String.format("RR selecciono: %s (quantum=%d)", 
                        next.getPid(), quantum));
        }
        
        return next;
    }
    
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        // Round Robin debe interrumpir cuando el quantum se agota
        if (currentQuantumRemaining <= 0) {
            Logger.exeLog("Quantum agotado para proceso " + current.getPid());
            return true;
        }
        return false;
    }
    
    //Decrementa el quantum cuando se ejecuta una unidad de tiempo
    //Porsiaco Este método debe ser llamado por el SyncController después de cada ejecución
     
    public void decrementaQuantum() {
        if (currentQuantumRemaining > 0) {
            currentQuantumRemaining--;
        }
    }
    
    //Verifica si el quantum se agotó

    public boolean isQuantumAgotado() {
        return currentQuantumRemaining <= 0;
    }
    
    //Reinicia el quantum para un nuevo proceso
    public void resetQuantum() {
        this.currentQuantumRemaining = quantum;
    }
    
    // Override para resetear el quantum cuando se confirma la selección
    @Override
    public synchronized void confirmProcessSelection(Process process) {
        if (process != null && readyQueue.remove(process)) {
            // Resetear quantum cuando se confirma la selección de un nuevo proceso
            currentQuantumRemaining = quantum;
            contextSwitch(process);
        }
    }
    
    @Override
    public String getAlgorithmName() {
        return "Round Robin (quantum=" + quantum + ")";
    }
    
    public int getQuantum() {
        return quantum;
    }
    
    public int getCurrentQuantumRemaining() {
        return currentQuantumRemaining;
    }
}