package model;


/*
Enum ProcessState
Representa los diferentes estados por los que pasa un proceso
mientras se ejecuta en el sistema.

ESTADOS:
  NEW: el proceso acaba de entrar.
  READY: está listo para usar la CPU.
  RUNNING: está ejecutándose.
  BLOCKED_MEMORY: detenido por un tema de memoria o fallo de página.
  BLOCKED_IO: esperando una operación de I/O.
  CONTEXT_SWITCHING: en pleno cambio de contexto.
  TERMINATED: ya terminó.

MÉTODOS ÚTILES:
  isBlocked():
    Dice si el proceso está bloqueado por memoria, I/O
    o cambio de contexto.

  isAvailable():
    Devuelve true si el proceso está en NEW o READY
    y puede ser elegido por el planificador.

  isActive():
    Indica si el proceso está corriendo o en cambio de contexto.

USO:
Sirve para controlar en qué estado está cada proceso
y para saber si puede ejecutarse, esperar o ya terminó.
*/


public enum ProcessState {
  NEW,
  READY,
  RUNNING,
  BLOCKED_MEMORY,
  BLOCKED_IO,
  CONTEXT_SWITCHING,
  TERMINATED;

  public boolean isBlocked() {
    return this == BLOCKED_IO || this == BLOCKED_MEMORY || 
           this == CONTEXT_SWITCHING; 
  }
  
  public boolean isAvailable() {
    return this == NEW || this == READY;
  }
  
  public boolean isActive() {
    return this == RUNNING || this == CONTEXT_SWITCHING;
  }
}