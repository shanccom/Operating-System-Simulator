package modules.scheduler;

import model.Burst;
import model.Process;
import utils.Logger;

/*Shortest Remaining Time First Scheduling
(SRT)

❑ Versión preemptive de SJF
❑ Cada vez que llega un nuevo proceso a la cola de espera, la decisión
sobre qué proceso planificar a continuación se vuelve a tomar
mediante el algoritmo SJF.
❑ ¿Es SRT más "óptima" que SJF en términos de tiempo medio de
espera mínimo para un conjunto determinado de procesos?
 */
public class SRT extends Scheduler {
    
    public SRT() {
        super();
        Logger.log("Planificador SRT inicializado (SJF expropiativo)");
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        Process shortest = null;
        int shortestRemainingTime = Integer.MAX_VALUE;
        
        // Buscar el proceso con el menor tiempo de CPU restante
        for (Process p : readyQueue) {
            Burst currentBurst = p.getCurrentBurst();
            
            if (currentBurst != null && currentBurst.isCPU()) {
                int remainingTime = currentBurst.getRemainingTime();
                
                if (remainingTime < shortestRemainingTime) {
                    shortestRemainingTime = remainingTime;
                    shortest = p;
                }
            }
        }
        
        if (shortest != null) {
            readyQueue.remove(shortest);
            contextSwitch(shortest);
            Logger.debug("SRTF seleccionó: " + shortest.getPid() + 
                        " con tiempo restante " + shortestRemainingTime);
        }
        
        return shortest;
    }
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        if (current == null || candidate == null) {
            return false;
        }
        
        Burst currentBurst = current.getCurrentBurst();
        Burst candidateBurst = candidate.getCurrentBurst();
        
        // Verificar que ambos tengan ráfagas de CPU válidas
        if (currentBurst == null || !currentBurst.isCPU() ||
            candidateBurst == null || !candidateBurst.isCPU()) {
            return false;
        }
        
        // Expropiar si el candidato tiene un menor tiempo restante
        int currentRemaining = currentBurst.getRemainingTime();
        int candidateRemaining = candidateBurst.getRemainingTime();
        
        if (candidateRemaining < currentRemaining) {
            Logger.debug("SRT: Expropiando " + current.getPid() + 
                        " (restante=" + currentRemaining + ") por " + 
                        candidate.getPid() + " (restante=" + candidateRemaining + ")");
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getAlgorithmName() {
        return "SRT (Shortest Remaining Time First)";
    }
}