package modules.sync;

import model.Process;
import java.util.List;

public interface SimulationStateListener {
    void onReadyQueueChanged(List<Process> readyQueue);
    void onBlockedIOChanged(List<Process> blockedIO);
    void onBlockedMemoryChanged(List<Process> blockedMemory);
    void onRunningChanged(Process runningProcess); 
    void onProcessStateChanged(Process process);
    void onTimeChanged(int currentTime);

    //para CPU
    void onProcessExecutionStarted(String pid, int startTime);
    void onProcessExecutionEnded(String pid, int endTime);
    void onContextSwitch(String pid, int startTime, int duration);

    //para I/O
    void onIOStarted(String pid, int startTime);
    void onIOEnded(String pid, int endTime);
    
}