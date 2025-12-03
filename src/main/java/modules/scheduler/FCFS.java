package modules.scheduler;

import model.Process;

/*
Implementacion del algoritmo FCFS (First Come First Served).
Atiende los procesos segun el orden de llegada, sin apropiacion.

selectNextProcess():
 - Si el proceso actual sigue ejecutando su rafaga, se mantiene.
 - Si termino o no tiene rafagas, se limpia y se toma el siguiente de la cola.

shouldPreempt():
 - FCFS nunca interrumpe procesos (retorna false).

getAlgorithmName():
 - Retorna el nombre del algoritmo.
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
            return currentProcess;
        }
        
        if (currentProcess != null && 
            (currentProcess.getState() == model.ProcessState.TERMINATED ||
            currentProcess.getCurrentBurst() == null ||
            currentProcess.getCurrentBurst().isCompleted())) {
            currentProcess = null;
        }
        
        return readyQueue.peek();
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