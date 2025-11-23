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
        Logger.log("Planificador FCFS inicializado");
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        // FCFS simplemente toma el primer proceso de la cola
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