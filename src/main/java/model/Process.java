package model;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
Clase Process
Representa un proceso dentro del simulador del sistema operativo.

OBJETIVO:
Modelar toda la información necesaria para ejecutar y monitorear un proceso:
ráfagas, tiempos, estado y manejo de memoria.

PRINCIPALES ELEMENTOS:

Datos básicos:
  pid: identificador del proceso.
  arrivalTime: tiempo de llegada.
  priority: prioridad asignada.
  bursts: lista de ráfagas CPU/IO.
  currentBurstIndex: índice de la ráfaga actual.

Estado y métricas:
  state: estado del proceso (NEW, READY, RUNNING…).
  startTime: primera ejecución.
  completionTime: finalización.
  waitingTime: tiempo total en READY.
  responseTime: tiempo hasta la primera ejecución.
  hasStarted: indica si ya comenzó a ejecutarse.

Memoria:
  requiredPages: páginas que necesita.
  loadedPages: páginas actualmente cargadas.
  pageFaults: cantidad de fallos de página.
  pageFaultEndTime: fin de atención de fallo.

Control del sistema:
  contextSwitchEndTime: fin del cambio de contexto.
  systemCallEndTime: fin de llamada al sistema.

MÉTODOS PRINCIPALES:
getCurrentBurst(): devuelve la ráfaga activa.
advanceBurst(): pasa a la siguiente ráfaga.
isCompleted(): verifica si terminó todas las ráfagas.
incrementWaitingTime(): suma tiempo en READY.
markFirstExecution(): registra el tiempo de respuesta.
loadPage(), unloadPage(), isPageLoaded(): manejo de páginas.
incrementPageFaults(): suma fallos.
getTurnaroundTime(): tiempo total del proceso.
getRemainingTime(): tiempo restante por ejecutar.

USO:
Se utiliza durante la simulación para planificar, calcular métricas
y administrar la memoria paginada del proceso.
*/


public class Process {

  private final String pid;
  private final int arrivalTime;
  private int priority;
  private final List<Burst> bursts;
  private int currentBurstIndex;
  private ProcessState state;
  private final int requiredPages;
  private final Set<Integer> loadedPages;
  private int pageFaults;
  private int startTime;           // Tiempo en que inicio su primera ejecucion
  private int completionTime;      // Tiempo en que termino
  private int waitingTime;         // Tiempo total en estado READY
  private int responseTime;        // Tiempo desde llegada hasta primera ejecucion
  private boolean hasStarted;      // Para calcular response time

  private int lastExecutionTime;   // Último momento en que se ejecuto 

  private int contextSwitchEndTime = 0;
  private int systemCallEndTime = -1;
  private int pageFaultEndTime = -1;

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
    this.startTime = -1;
    this.completionTime = -1;
    this.responseTime = -1;
  }

  public Burst getCurrentBurst() {
    if (currentBurstIndex < bursts.size()) {
      return bursts.get(currentBurstIndex);
    }
    return null;
  }

  public void advanceBurst() {
    currentBurstIndex++;
  }

  public boolean isCompleted() {
    return currentBurstIndex >= bursts.size();
  }

  public int getTotalCPUTime() {
      return bursts.stream().filter(Burst::isCPU).mapToInt(Burst::getDuration).sum();
  }

  public int getTotalBurstTime() {
      return bursts.stream().mapToInt(Burst::getDuration).sum();
  }

  public int getTurnaroundTime() {
    if (completionTime < 0) 
      return -1;
    return completionTime - arrivalTime;
  }

  public void markFirstExecution(int currentTime) {
    if (!hasStarted) {
      this.hasStarted = true;
      this.startTime = currentTime;
      this.responseTime = currentTime - arrivalTime;
    }
  }
  //se esta verificando antes
  public void incrementWaitingTime() {
    
      waitingTime++;
    
  }

  public void loadPage(int pageNumber) {
    loadedPages.add(pageNumber);
  }

  public void unloadPage(int pageNumber) {
    loadedPages.remove(pageNumber);
  }

  public boolean isPageLoaded(int pageNumber) {
    return loadedPages.contains(pageNumber);
  }

  public void incrementPageFaults() {
    pageFaults++;
  }

  public void clearLoadedPages() {
    loadedPages.clear();
  }

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

  public int getSystemCallEndTime() { 
    return systemCallEndTime; 
  }

  public void setSystemCallEndTime(int time) { 
    this.systemCallEndTime = time; 
  }

  public boolean isInSystemCall() { 
    return systemCallEndTime > 0; 
  }

  public void clearSystemCall() {
    systemCallEndTime = -1;
  }

  public int getPageFaultEndTime() { 
    return pageFaultEndTime; 
  }

  public void setPageFaultEndTime(int time) { 
      this.pageFaultEndTime = time; 
  }

  public boolean isWaitingForPageFault() { 
      return pageFaultEndTime > 0; 
  }

  public void clearPageFault() {
      pageFaultEndTime = -1;
  }
  public int getContextSwitchEndTime() {
    return contextSwitchEndTime;
  }

  public void setContextSwitchEndTime(int endTime) {
    this.contextSwitchEndTime = endTime;
  }

  public void clearContextSwitch() {
    this.contextSwitchEndTime = 0;
  }

  public boolean isInContextSwitch() {
    return getState() == ProcessState.CONTEXT_SWITCHING;
  }

}