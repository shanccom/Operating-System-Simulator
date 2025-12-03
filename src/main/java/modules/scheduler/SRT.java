package modules.scheduler;

import model.Burst;
import model.Process;
import utils.Logger;

/*
Shortest Remaining Time First (SRT)

Version apropiativa de SJF. Cada vez que un proceso llega a la cola READY, 
se vuelve a evaluar cual proceso debe ejecutarse usando la logica de SJF.

Caracteristicas:
- Selecciona el proceso con el menor tiempo de CPU restante.
- Es apropiativo: puede interrumpir al proceso actual si llega uno mas corto.

Comparacion:
- SJF: Optimo solo si todos los procesos llegan al mismo tiempo.
- SRT: Optimo globalmente porque siempre elige el trabajo mas corto disponible.

Trade-off:
- SRT genera mas cambios de contexto que SJF.
*/

public class SRT extends Scheduler {
    
    public SRT() {
        super();
        Logger.exeLog("Planificador SRT inicializado (SJF expropiativo)");
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
            //readyQueue.remove(shortest);
            //contextSwitch(shortest);
            Logger.procLog("SRTF seleccionó: " + shortest.getPid() + 
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
            Logger.exeLog("SRT: Expropiando " + current.getPid() + 
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