package modules.memory;

public interface MemoryEventListener {
    void onPageAccess(int frameIndex, String pid, int page, boolean hit);        // acceso (hit)
    void onPageFault(String pid, int page);                                      // page fault
    void onFrameLoaded(int frameIndex, String pid, int page);                    // carga en frame
    void onFrameEvicted(int frameIndex, String oldPid, int oldPage);            // eviction
    void onVictimChosen(int frameIndex, String reason);                         // candidato (resaltar)
    void onSnapshot(String snapshot);                              // snapshot textual opcional
}
