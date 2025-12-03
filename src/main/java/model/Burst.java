package model;

/*
Clase Burst
Representa una rafaga de CPU o IO dentro de un proceso.
Cada rafaga tiene un tipo, una duracion total y un tiempo restante.

PARTES PRINCIPALES:

type:
  Indica si la rafaga es CPU o IO.

duration:
  Tiempo total que la rafaga debe ejecutarse.

remainingTime:
  Tiempo que falta para completarla.

METODOS:
execute(time):
  Reduce el tiempo restante segun el tiempo ejecutado.
  Devuelve true si la rafaga ya termino.

isCompleted():
  Indica si remainingTime llego a cero.

reset():
  Reinicia el tiempo restante al valor original.

isCPU() / isIO():
  Indican si la rafaga es de CPU o de IO.

OBJETIVO:
Modelar la unidad basica de trabajo de un proceso en un simulador
de sistemas operativos.
*/


public class Burst {
    
  public enum BurstType {
    CPU,  // Rafaga de CPU
    IO    // Rafaga de E/S
  }
  
  private final BurstType type;
  private final int duration;        // Duracion total en unidades de tiempo
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