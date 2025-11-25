package model;

public class Config {
  
  public enum SchedulerType {
      FCFS,        // First Come First Served
      SJF,         // Shortest Job First
      ROUND_ROBIN, // Round Robin
      PRIORITY     // Por Prioridades (opcional)
  }
  
  public enum ReplacementType {
      FIFO,     // First In First Out
      LRU,      // Least Recently Used
      OPTIMAL   // Algoritmo Óptimo
      NRU   // Algoritmo Óptimo
  }
  
  private int totalFrames;
  private int frameSize;  // Tamaño de cada marco
  
  private SchedulerType schedulerType;
  private int quantum;  // Para Round Robin
  
  private ReplacementType replacementType;
  
  private boolean enableIO;
  private int timeUnit;
  
  public Config() {
      this.totalFrames = 10;
      this.frameSize = 4096;
      this.schedulerType = SchedulerType.FCFS;
      this.quantum = 2;
      this.replacementType = ReplacementType.FIFO;
      this.enableIO = false;
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
      return enableIO;
  }
  
  public void setEnableIO(boolean enableIO) {
      this.enableIO = enableIO;
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
          totalFrames, schedulerType, replacementType, quantum, enableIO
      );
  }
  
  public boolean validate() {
      if (totalFrames <= 0) return false;
      if (schedulerType == SchedulerType.ROUND_ROBIN && quantum <= 0) return false;
      return true;
  }
}