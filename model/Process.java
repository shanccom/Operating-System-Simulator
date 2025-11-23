package model;

import java.util.List;

public class Process {
    private String pid;                 
    private List<Integer> pagesNeeded;  
    private int arrivalTime;            
    private int priority;               

    // Métricas de memoria
    private int pageFaults = 0;
    private int replacements = 0;

    public Process(String pid, List<Integer> pagesNeeded, int arrivalTime, int priority) {
        this.pid = pid;
        this.pagesNeeded = pagesNeeded;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
    }

    public String getPid() { return pid; }
    public List<Integer> getPagesNeeded() { return pagesNeeded; }
    public int getArrivalTime() { return arrivalTime; }
    public int getPriority() { return priority; }

    // Métricas de memotiea 
    public void addPageFault() { pageFaults++; }
    public void addReplacement() { replacements++; }
    public int getPageFaults() { return pageFaults; }
    public int getReplacements() { return replacements; }

    @Override
    public String toString() {
        return "Process{" +
                "pid='" + pid + '\'' +
                ", pagesNeeded=" + pagesNeeded +
                ", pageFaults=" + pageFaults +
                ", replacements=" + replacements +
                '}';
    }
}