package modules.scheduler;

import model.Burst;
import model.Process;
import utils.Logger;


/*
Implementacion del algoritmo de planificacion por Prioridad.
Permite modo expropiativo o no expropiativo segun el valor recibido en el constructor.

selectNextProcess():
 - Busca en la cola el proceso con mayor prioridad (numero mas pequeño).
 - En caso de empate, elige el que llego primero.
 - Devuelve el proceso con mejor prioridad sin removerlo de la cola.

shouldPreempt():
 - Solo funciona en modo expropiativo.
 - Expropia si el proceso candidato tiene mayor prioridad que el actual.

getAlgorithmName():
 - Retorna el nombre del algoritmo indicando si es preemptive o non-preemptive.
*/


public class Priority extends Scheduler {
    
    private final boolean isPreemptive;

    public Priority() {
        this(false);
    }
    
    //true para modo expropiativo, false para no expropiativo
    public Priority(boolean preemptive) {
        super();
        this.isPreemptive = preemptive;
        Logger.exeLog("Planificador Priority inicializado " + 
                  (preemptive ? "(expropiativo)" : "(no expropiativo)"));
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
            //readyQueue.remove(highestPriority);
            //contextSwitch(highestPriority);
            Logger.exeLog("Priority seleccionó: " + highestPriority.getPid() + 
                        " con prioridad " + bestPriority);
        }
        
        return highestPriority;
    }
    
    //Decide si debe expropiar el proceso actual
    //Solo aplica si estaen modo expropiativo
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        if (!isPreemptive || current == null || candidate == null) {
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
        return "Priority " + (isPreemptive ? "(Preemptive)" : "(Non-Preemptive)");
    }
    
    public boolean isPreemptive() {
        return isPreemptive;
    }
}