package modules.memory;

public interface MemoryEventListener {
    void onPageAccess(int frameIndex, String pid, int page, boolean hit);        // acceso (hit)
    void onPageFault(String pid, int page);                                      // page fault
    void onFrameLoaded(int frameIndex, String pid, int page, long lastAccessTime);                    // carga en frame
    void onFrameEvicted(int frameIndex, String oldPid, int oldPage);            // eviction
    void onVictimChosen(int frameIndex, String reason, long lastAccessTime);                         // candidato (resaltar)
    void onSnapshot(String snapshot);                              // snapshot textual opcional
    void onPageAccessed(int frameIndex, String pid, int page, long newAccessTime); // actualizar lastAccessTime durante
                                                                                   // ejecución 
}
