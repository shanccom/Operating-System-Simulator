package modules.scheduler;

import model.Process;
import utils.Logger;

/**
 * First Come First Served (FCFS)
 * Algoritmo no apropiativo que ejecuta procesos en orden de llegada
 */
public class FCFS extends Scheduler {
    
    public FCFS() {
        super();
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        if (currentProcess != null && 
            currentProcess.getState() == model.ProcessState.RUNNING &&
            currentProcess.getCurrentBurst() != null &&
            !currentProcess.getCurrentBurst().isCompleted()) {
            Logger.debug("FCFS continúa con: " + currentProcess.getPid());
            return currentProcess; 
        }
        
        
        if (currentProcess != null && 
            (currentProcess.getState() == model.ProcessState.TERMINATED ||
            (currentProcess.getCurrentBurst() != null && currentProcess.getCurrentBurst().isCompleted()))) {
            currentProcess = null;
        }
        
      
        Process next = readyQueue.poll();
        if (next != null) {
            contextSwitch(next);
            Logger.debug("FCFS seleccionó: " + next.getPid());
        }
        
        return next;
    }
    
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        // FCFS nunca es apropiativo
        return false;
    }
    
    @Override
    public String getAlgorithmName() {
        return "FCFS (First Come First Served)";
    }
}