package modules.scheduler;

import model.Burst;
import model.Process;
import utils.Logger;

/* Shortest Job First (SJF)
   Algoritmo no apropiativo que ejecuta el proceso con la ráfaga de CPU más corta
*/
public class SJF extends Scheduler {
    
    public SJF() {
        super();
        Logger.log("Planificador SJF inicializado");
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        Process shortest = null;
        int shortestBurstTime = Integer.MAX_VALUE;
        
        // Buscar en la cola el proceso con menor tiempo de CPU restante
        for (Process p : readyQueue) {
            Burst currentBurst = p.getCurrentBurst();
            
        }
        
        
        return shortest;
    }
    
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        return false;
    }
    
    @Override
    public String getAlgorithmName() {
        return "SJF (Shortest Job First)";
    }
    
}