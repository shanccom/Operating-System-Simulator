package modules.scheduler;
import model.Burst;
import model.Process;
import utils.Logger;


/*
Implementacion del algoritmo Priority en modo expropiativo.
Selecciona siempre el proceso con mayor prioridad (numero mas pequeno).

selectNextProcess():
 - Recorre la cola y elige el proceso con mejor prioridad.
 - Si hay empate, selecciona el que llego primero.
 - Solo devuelve el proceso sin removerlo.

shouldPreempt():
 - Expropia si el proceso candidato tiene mayor prioridad que el actual.

getAlgorithmName():
 - Retorna el nombre del algoritmo en modo preemptive.
*/


public class PriorityPreemptive extends Scheduler {
    
    public PriorityPreemptive() {
        super();
        Logger.exeLog("Planificador Priority inicializado (expropiativo)");
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        Process highestPriority = null;
        int bestPriority = Integer.MAX_VALUE; // menor numero = mayor prioridad
        
        // Buscar el proceso con la mayor prioridad (menor número)
        for (Process p : readyQueue) {
            int priority = p.getPriority();
            // Desempate por orden de llegada (FCFS)
            if (priority < bestPriority || 
                (priority == bestPriority && 
                 highestPriority != null && 
                 p.getArrivalTime() < highestPriority.getArrivalTime())) {
                bestPriority = priority;
                highestPriority = p;
            }
        }
        
        if (highestPriority != null) {
            Logger.exeLog("Priority seleccionó: " + highestPriority.getPid() + 
                         " con prioridad " + bestPriority);
        }
        
        return highestPriority;
    }
    
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        if (current == null || candidate == null) {
            return false;
        }
        
        // Expropiar si el candidato tiene MAYOR prioridad (menor número)
        if (candidate.getPriority() < current.getPriority()) {
            Logger.exeLog("Priority: Expropiando " + current.getPid() + 
                         " (prioridad=" + current.getPriority() + ") por " + 
                         candidate.getPid() + " (prioridad=" + candidate.getPriority() + ")");
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Priority (Preemptive)";
    }
}