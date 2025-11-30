package model;

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