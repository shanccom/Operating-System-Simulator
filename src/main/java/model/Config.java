package model;

/*
Clase Config
Administra todos los parametros de configuracion del sistema de simulacion.

OBJETIVO:
Centralizar opciones para el planificador, memoria, penalidades y tamanos.

ELEMENTOS PRINCIPALES:

SchedulerType:
  Define el tipo de planificador usado:
  FCFS, SJF, SRT, ROUND_ROBIN, PRIORITY, PRIORITYPREEMPTIVE.

ReplacementType:
  Define el algoritmo de reemplazo de paginas:
  FIFO, LRU, OPTIMAL, NRU.

Atributos configurables:
  totalFrames: cantidad total de marcos de memoria.
  frameSize: tamano de cada marco.
  schedulerType: planificador seleccionado.
  quantum: usado solo si el planificador es Round Robin.
  replacementType: algoritmo para manejo de paginas.
  ENABLE_IO: bandera global para permitir operaciones de IO.
  timeUnit: tiempo base usado por la simulacion.

Overheads:
  systemCallOverhead: costo de llamada al sistema.
  pageFaultPenalty: penalizacion por fallo de pagina.
  contextSwitchOverhead: costo por cambio de contexto.

Metodos:
  getters/setters para modificar configuraciones.
  validate():
    Verifica que los valores basicos sean correctos.
  toString():
    Devuelve un resumen legible de la configuracion.

Uso:
Proveer configuracion global a los modulos de CPU, memoria, IO y planificacion.
*/

public class Config {
  
  public enum SchedulerType {
      FCFS,        // First Come First Served
      SJF,         // Shortest Job First
      SRT,
      ROUND_ROBIN, // Round Robin
      PRIORITY,     // Por Prioridades (opcional)
      PRIORITYPREEMPTIVE
  }
  
  public enum ReplacementType {
      FIFO,     // First In First Out
      LRU,      // Least Recently Used
      OPTIMAL,   // Algoritmo Óptimo
      NRU   // Algoritmo Óptimo
  }
  
  private int totalFrames;
  private int frameSize;
  private SchedulerType schedulerType;
  private int quantum;  // Para Round Robin
  private ReplacementType replacementType;
  private static final boolean ENABLE_IO = true;
  private int timeUnit;
  
  private int systemCallOverhead = 1;
  private int pageFaultPenalty = 0;
  private int contextSwitchOverhead = 1;

  public Config() { //Por defecto
      this.totalFrames = 10;
      this.frameSize = 4096;
      this.schedulerType = SchedulerType.FCFS;
      this.quantum = 2;
      this.replacementType = ReplacementType.FIFO;
      this.timeUnit = 100;
  }
  
  public Config(int totalFrames, SchedulerType schedulerType, 
                ReplacementType replacementType, int quantum) {
      this();
      this.totalFrames = totalFrames;
      this.schedulerType = schedulerType;
      this.replacementType = replacementType;
      this.quantum = quantum;
  }
  
  public int getTotalFrames() {
      return totalFrames;
  }
  
  public void setTotalFrames(int totalFrames) {
      if (totalFrames <= 0) {
          throw new IllegalArgumentException("El numero de frames debe de ser positivos");
      }
      this.totalFrames = totalFrames;
  }
  
  public int getFrameSize() {
      return frameSize;
  }
  
  public void setFrameSize(int frameSize) {
      this.frameSize = frameSize;
  }
  
  public SchedulerType getSchedulerType() {
      return schedulerType;
  }
  
  public void setSchedulerType(SchedulerType schedulerType) {
      this.schedulerType = schedulerType;
  }
  
  public int getQuantum() {
      return quantum;
  }
  
  public void setQuantum(int quantum) {
      if (quantum <= 0) {
          throw new IllegalArgumentException("El Quantum debe de ser positivo");
      }
      this.quantum = quantum;
  }
  
  public ReplacementType getReplacementType() {
      return replacementType;
  }
  
  public void setReplacementType(ReplacementType replacementType) {
      this.replacementType = replacementType;
  }
  
  public boolean isEnableIO() {
      return ENABLE_IO;
  }
  
  public int getTimeUnit() {
      return timeUnit;
  }
  
  public void setTimeUnit(int timeUnit) {
      this.timeUnit = timeUnit;
  }
  
  @Override
  public String toString() {
      return String.format(
          "Config[Frames=%d, Scheduler=%s, Replacement=%s, Quantum=%d, IO=%s]",
          totalFrames, schedulerType, replacementType, quantum
      );
  }
  
  public boolean validate() {
      if (totalFrames <= 0) return false;
      if (schedulerType == SchedulerType.ROUND_ROBIN && quantum <= 0) return false;
      return true;
  }

  public int getSystemCallOverhead() { 
    return systemCallOverhead; 
  }
  
  public void setSystemCallOverhead(int overhead) { 
    this.systemCallOverhead = overhead; 
  }

  public int getPageFaultPenalty() { 
    return pageFaultPenalty; 
  }
  
  public void setPageFaultPenalty(int penalty) { 
    this.pageFaultPenalty = penalty; 
  }

  public int getContextSwitchOverhead() {
    return contextSwitchOverhead;
  }
  
  public void setContextSwitchOverhead(int overhead) {
    this.contextSwitchOverhead = overhead;
  }

}