package model;

/**
 * Estados de un proceso
**/

public enum ProcessState {
  NEW,
  READY,
  RUNNING,
  BLOCKED_MEMORY,
  BLOCKED_IO,
  TERMINATED;

  public boolean isBlocked() {
      return this == BLOCKED_MEMORY || this == BLOCKED_IO;
  }
  
  public boolean isActive() {
      return this != TERMINATED;
  }
}