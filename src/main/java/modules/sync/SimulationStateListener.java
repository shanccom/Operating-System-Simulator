package modules.sync;

import model.Process;
import java.util.List;

public interface SimulationStateListener {
    void onReadyQueueChanged(List<Process> readyQueue);
    void onBlockedIOChanged(List<Process> blockedIO);
    void onBlockedMemoryChanged(List<Process> blockedMemory);
    void onProcessStateChanged(Process process);
    void onTimeChanged(int currentTime);

     
    void onProcessExecutionStarted(String pid, int startTime);
    void onProcessExecutionEnded(String pid, int endTime);
    void onContextSwitch();
    
}