package modules.scheduler;

import model.Process;
import model.ProcessState;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Clase abstracta base para todos los algoritmos de planificacion
 * Define la interfaz comun y comportamientos compartidos
 */
public abstract class Scheduler {
    
    protected final Queue<Process> readyQueue;
    protected Process currentProcess;
    protected int currentTime;
    protected int contextSwitches;
    
    // Metricas del sistema
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
    
    /**
     * Anade un proceso a la cola de listos (thread-safe)
     */
    public synchronized void addProcess(Process process) {
    // Aceptar procesos que necesiten ir a READY
        if (process.getState() != ProcessState.TERMINATED && 
            process.getState() != ProcessState.RUNNING) {
            
            process.setState(ProcessState.READY);
            readyQueue.offer(process);
            Logger.debug("Proceso " + process.getPid() + " añadido a cola READY");
            notifyAll();
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
        if (currentProcess != null && currentProcess != newProcess) {
            contextSwitches++;
            Logger.debug("Context switch: " + 
                (currentProcess != null ? currentProcess.getPid() : "null") + 
                " -> " + newProcess.getPid());
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
    
    public Process getCurrentProcess() {
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
    
    /**
     * Imprime el reporte de metricas
     */
    public void printMetrics() {
        Logger.separator();
        Logger.section("METRICAS DEL PLANIFICADOR - " + getAlgorithmName());
        Logger.log("Procesos completados: " + completedProcesses);
        Logger.log(String.format("Tiempo promedio de espera: %.2f", getAverageWaitingTime()));
        Logger.log(String.format("Tiempo promedio de retorno: %.2f", getAverageTurnaroundTime()));
        Logger.log(String.format("Tiempo promedio de respuesta: %.2f", getAverageResponseTime()));
        Logger.log(String.format("Utilizacion de CPU: %.2f%%", getCPUUtilization()));
        Logger.log("Cambios de contexto: " + contextSwitches);
        Logger.log("Tiempo total de CPU: " + totalCPUTime);
        Logger.log("Tiempo inactivo: " + idleTime);
        Logger.separator();
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