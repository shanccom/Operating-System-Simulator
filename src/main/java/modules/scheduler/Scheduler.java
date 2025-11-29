package modules.scheduler;

import model.Process;
import model.ProcessState;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Scheduler {
    
    protected final Queue<Process> readyQueue;
    protected Process currentProcess;
    protected int currentTime;
    protected int contextSwitches;
    
    protected double totalWaitingTime;
    protected double totalTurnaroundTime;
    protected double totalResponseTime;
    protected int completedProcesses;
    protected int totalCPUTime;
    protected int idleTime;
    
    public Scheduler() {
        this.readyQueue = new ConcurrentLinkedQueue<>();
        this.currentProcess = null;
        this.currentTime = 0;
        this.contextSwitches = 0;
        this.totalWaitingTime = 0;
        this.totalTurnaroundTime = 0;
        this.totalResponseTime = 0;
        this.completedProcesses = 0;
        this.totalCPUTime = 0;
        this.idleTime = 0;
    }
    

    public synchronized void addProcess(Process process) {
      if (process.getState() == ProcessState.TERMINATED || 
          process.getState() == ProcessState.RUNNING) {
          Logger.debug("[SCHEDULER] No se puede agregar proceso " + process.getPid() + 
                      " en estado: " + process.getState());
          return;
      }
      
      if (readyQueue.contains(process)) {
          Logger.debug("[SCHEDULER] Proceso " + process.getPid() + " ya está en cola READY");
          return;
      }
      
      readyQueue.offer(process);
      Logger.debug("[SCHEDULER] " + process.getPid() + " agregado (cola: " + readyQueue.size() + ")");
      
      notifyAll();
    }
    
    public synchronized void confirmProcessSelection(Process process) {
      // Remover el proceso de la cola sin importar su posición
      if (process != null && readyQueue.remove(process)) {
        contextSwitch(process);
      }
    }

    /**
     * Selecciona el siguiente proceso a ejecutar
     * DEBE ser implementado por cada algoritmo
     */
    public abstract Process selectNextProcess();
    
    /**
     * Verifica si el proceso actual debe ser reemplazado
     * Para algoritmos apropiativos como Round Robin
     */
    public abstract boolean shouldPreempt(Process current, Process candidate);
    
    /**
     * Obtiene el nombre del algoritmo
     */
    public abstract String getAlgorithmName();
    
    /**
     * Ejecuta el cambio de contexto
     */
    protected void contextSwitch(Process newProcess) {
        if (currentProcess != null && 
            newProcess != null && 
            !currentProcess.equals(newProcess) &&
            currentProcess.getState() != ProcessState.TERMINATED) {
            
            contextSwitches++;
            Logger.log("Context switch: " + currentProcess.getPid() + " -> " + newProcess.getPid());
        }
        
        currentProcess = newProcess;
    }
    
    /**
     * Actualiza las metricas cuando un proceso se completa
     */
    public void onProcessComplete(Process process) {
        completedProcesses++;
        totalWaitingTime += process.getWaitingTime();
        totalTurnaroundTime += process.getTurnaroundTime();
        totalResponseTime += process.getResponseTime();
        
        Logger.log("Proceso " + process.getPid() + " completado en t=" + currentTime);
    }
    
    protected void updateCurrentProcess(Process process) {
      this.currentProcess = process;
    }

    public void incrementTime() {
        currentTime++;
    }
    
    public void recordCPUTime(int time) {
        totalCPUTime += time;
    }
    
    public void recordIdleTime(int time) {
        idleTime += time;
    }
    
    // Getters para metricas
    
    public double getAverageWaitingTime() {
        return completedProcesses > 0 ? totalWaitingTime / completedProcesses : 0;
    }
    
    public double getAverageTurnaroundTime() {
        return completedProcesses > 0 ? totalTurnaroundTime / completedProcesses : 0;
    }
    
    public double getAverageResponseTime() {
        return completedProcesses > 0 ? totalResponseTime / completedProcesses : 0;
    }
    
    public double getCPUUtilization() {
        int totalTime = totalCPUTime + idleTime;
        return totalTime > 0 ? (double) totalCPUTime / totalTime * 100 : 0;
    }
    
    public int getContextSwitches() {
        return contextSwitches;
    }
    
    public int getCurrentTime() {
        return currentTime;
    }
    
    public void setCurrentTime(int time) {
        this.currentTime = time;
    }
    
    public void setCurrentProcess(Process process) {
      this.currentProcess = process;
    }
    
    public Process getCurrentProcess() {
      if (currentProcess != null && currentProcess.getState() == ProcessState.TERMINATED) {
        currentProcess = null;
      }
      
      return currentProcess;
    }
    
    public synchronized List<Process> getReadyQueueSnapshot() {
        return new ArrayList<>(readyQueue);
    }
    public Process peekNextProcess() {
        return readyQueue.peek();
    }

    public synchronized int getReadyQueueSize() {
        return readyQueue.size();
    }
    
    public synchronized boolean hasReadyProcesses() {
        return !readyQueue.isEmpty();
    }

    public void forceContextSwitch() {
      currentProcess = null;
    }
    
    /**
     * Imprime el reporte de metricas
     */
    public void printMetrics() {
        System.out.println();
        Logger.log("[SCHE] METRICAS DEL PLANIFICADOR - " + getAlgorithmName());
        Logger.log("Procesos completados: " + completedProcesses);
        Logger.log(String.format("Tiempo promedio de espera: %.2f", getAverageWaitingTime()));
        Logger.log(String.format("Tiempo promedio de retorno: %.2f", getAverageTurnaroundTime()));
        Logger.log(String.format("Tiempo promedio de respuesta: %.2f", getAverageResponseTime()));
        Logger.log(String.format("Utilizacion de CPU: %.2f%%", getCPUUtilization()));
        Logger.log("Cambios de contexto: " + contextSwitches);
        Logger.log("Tiempo total de CPU: " + totalCPUTime);
        Logger.log("Tiempo inactivo: " + idleTime);
        System.out.println();
    }
    
    /**
     * Resetea el planificador para una nueva simulacion
     */
    public synchronized void reset() {
        readyQueue.clear();
        currentProcess = null;
        currentTime = 0;
        contextSwitches = 0;
        totalWaitingTime = 0;
        totalTurnaroundTime = 0;
        totalResponseTime = 0;
        completedProcesses = 0;
        totalCPUTime = 0;
        idleTime = 0;
        Logger.log("Planificador reseteado");
    }
}