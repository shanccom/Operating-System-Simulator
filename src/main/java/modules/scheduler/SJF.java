package modules.scheduler;

import model.Burst;
import model.Process;
import utils.Logger;

/*
Implementacion del algoritmo SJF (Shortest Job First).
Selecciona el proceso con la rafaga de CPU mas corta y no es apropiativo.

selectNextProcess():
 - Recorre la cola READY y elige el proceso con menor tiempo de CPU restante.

shouldPreempt():
 - Siempre retorna false porque SJF no interrumpe procesos.

getAlgorithmName():
 - Retorna el nombre del algoritmo.
*/

public class SJF extends Scheduler {
    
    public SJF() {
        super();
        Logger.exeLog("Planificador SJF inicializado");
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

            if(currentBurst!=null &&currentBurst.isCPU()){
                int burstTime = currentBurst.getRemainingTime();
                if(burstTime< shortestBurstTime){
                    shortestBurstTime = burstTime;
                    shortest = p;
                }
            }
            
        }

        if(shortest != null){
            //readyQueue.remove(shortest);
            //contextSwitch(shortest);
            Logger.procLog("SJF seleccionó: " + shortest.getPid() + " con ráfaga restante " + shortestBurstTime);
        }
        
        
        return shortest;
    }
    
    //No es apropiativo
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        return false;
    }
    
    @Override
    public String getAlgorithmName() {
        return "SJF (Shortest Job First)";
    }
    
}