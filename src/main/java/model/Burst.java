package model;

public class Burst {
    
  public enum BurstType {
    CPU,  // Ráfaga de CPU
    IO    // Ráfaga de E/S
  }
  
  private final BurstType type;
  private final int duration;        // Duración total en unidades de tiempo
  private int remainingTime;         // Tiempo restante para completar
  
  public Burst(BurstType type, int duration) {
    this.type = type;
    this.duration = duration;
    this.remainingTime = duration;
  }
  
  public boolean execute(int time) {
    remainingTime = Math.max(0, remainingTime - time);
    return isCompleted();
  }
  
  public boolean isCompleted() {
    return remainingTime <= 0;
  }
  
  public void reset() {
    this.remainingTime = this.duration;
  }
  
  public BurstType getType() {
    return type;
  }
  
  public int getDuration() {
    return duration;
  }
  
  public int getRemainingTime() {
    return remainingTime;
  }
  
  public boolean isCPU() {
    return type == BurstType.CPU;
  }
  
  public boolean isIO() {
    return type == BurstType.IO;
  }
  
  @Override
  public String toString() {
    return String.format("%s(%d)", type, duration);
  }
  
  public Burst copy() {
    Burst copy = new Burst(this.type, this.duration);
    copy.remainingTime = this.remainingTime;
    return copy;
  }
}