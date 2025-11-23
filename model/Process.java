package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Proceso con informacion para el sistema (de ser el caso que me olvide de agregar algo ponganlo y avisen porfa)
**/

public class Process {
  
  private final String pid;
  private final int arrivalTime;
  private int priority;
  
  // Ráfagas de ejecución (CPU y E/S)
  private final List<Burst> bursts;
  private int currentBurstIndex;
  
  // Estado del proceso
  private ProcessState state;
  
  // Memoria
  private final int requiredPages;
  private final Set<Integer> loadedPages;  // Páginas actualmente en memoria
  private int pageFaults;
  
  // Métricas de tiempo
  private int startTime;           // Tiempo en que inició su primera ejecución
  private int completionTime;      // Tiempo en que terminó
  private int waitingTime;         // Tiempo total en estado READY
  private int responseTime;        // Tiempo desde llegada hasta primera ejecución
  private boolean hasStarted;      // Para calcular response time
  
  private int lastExecutionTime;   // Último momento en que se ejecutó (para cálculos)
  
  public Process(String pid, int arrivalTime, List<Burst> bursts, int priority, int requiredPages) {
      this.pid = pid;
      this.arrivalTime = arrivalTime;
      this.bursts = new ArrayList<>(bursts);
      this.priority = priority;
      this.requiredPages = requiredPages;
      
      this.currentBurstIndex = 0;
      this.state = ProcessState.NEW;
      this.loadedPages = new HashSet<>();
      this.pageFaults = 0;
      this.waitingTime = 0;
      this.hasStarted = false;
      this.lastExecutionTime = arrivalTime;
  }
  
  // Rafaga actual que debe de ejecutarse
  public Burst getCurrentBurst() {
      if (currentBurstIndex < bursts.size()) {
          return bursts.get(currentBurstIndex);
      }
      return null;
  }
  
  // Pasa a la siguiente rafaga
  public void advanceBurst() {
      currentBurstIndex++;
  }
  
  // Verifica si todas las rafagashan sido completadas
  public boolean isCompleted() {
      return currentBurstIndex >= bursts.size();
  }
  
  // Tiempo total de las rafas 
  public int getTotalCPUTime() {
      return bursts.stream()
              .filter(Burst::isCPU)
              .mapToInt(Burst::getDuration)
              .sum();
  }
  
  // Obtiene el tiempo total de todas las ráfagas
  public int getTotalBurstTime() {
      return bursts.stream()
              .mapToInt(Burst::getDuration)
              .sum();
  }
  
  // Calcula el tiempo de retorno (turnaround time)
  public int getTurnaroundTime() {
      return completionTime - arrivalTime;
  }
  
  // Registra el inicio de la primera ejecución
  public void markFirstExecution(int currentTime) {
      if (!hasStarted) {
          this.hasStarted = true;
          this.startTime = currentTime;
          this.responseTime = currentTime - arrivalTime;
      }
  }
  
  // Actualiza el tiempo de espera cuando el proceso está en READY
  public void updateWaitingTime(int currentTime) {
      if (state == ProcessState.READY) {
          waitingTime += (currentTime - lastExecutionTime);
      }
      lastExecutionTime = currentTime;
  }
  
  // Gestión de memoria
  
  // Carga la pagina a memoria
  public void loadPage(int pageNumber) {
      loadedPages.add(pageNumber);
  }
  
  // Descarga una pagina de memoria
  public void unloadPage(int pageNumber) {
      loadedPages.remove(pageNumber);
  }
  
  // Verifica si la pagina esta cargada
  public boolean isPageLoaded(int pageNumber) {
      return loadedPages.contains(pageNumber);
  }
  
  // Incrementa el contador de fallos de pagina
  public void incrementPageFaults() {
      pageFaults++;
  }
  
  public void clearLoadedPages() {
      loadedPages.clear();
  }
  
  // Getters y Setters
  
  public String getPid() {
      return pid;
  }
  
  public int getArrivalTime() {
      return arrivalTime;
  }
  
  public int getPriority() {
      return priority;
  }
  
  public void setPriority(int priority) {
      this.priority = priority;
  }
  
  public ProcessState getState() {
      return state;
  }
  
  public void setState(ProcessState state) {
      this.state = state;
  }
  
  public int getRequiredPages() {
      return requiredPages;
  }
  
  public Set<Integer> getLoadedPages() {
      return new HashSet<>(loadedPages);
  }
  
  public int getPageFaults() {
      return pageFaults;
  }
  
  public int getCompletionTime() {
      return completionTime;
  }
  
  public void setCompletionTime(int completionTime) {
      this.completionTime = completionTime;
  }
  
  public int getWaitingTime() {
      return waitingTime;
  }
  
  public int getResponseTime() {
      return responseTime;
  }
  
  public int getStartTime() {
      return startTime;
  }
  
  public List<Burst> getBursts() {
      return new ArrayList<>(bursts);
  }
  
  public int getCurrentBurstIndex() {
      return currentBurstIndex;
  }
  
  public int getRemainingTime() {
      int remaining = 0;
      for (int i = currentBurstIndex; i < bursts.size(); i++) {
          remaining += bursts.get(i).getRemainingTime();
      }
      return remaining;
  }
  
  @Override
  public String toString() {
      return String.format("Process[%s, Arrival=%d, Priority=%d, State=%s, Pages=%d]",
              pid, arrivalTime, priority, state, requiredPages);
  }
  
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Process process = (Process) o;
      return pid.equals(process.pid);
  }
  
  @Override
  public int hashCode() {
      return pid.hashCode();
  }
}