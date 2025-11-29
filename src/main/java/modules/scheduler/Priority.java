package modules.scheduler;

import model.Burst;
import model.Process;
import utils.Logger;

/*Priority Scheduling
Se asocia un número de prioridad (entero) a cada proceso
EL planificador asigna el procesador al proceso con la más alta prioridad
• Preemptive
• No preemptive
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