package modules.memory;

import model.Process;
import utils.Logger;


public class LRU extends MemoryManager {

    public LRU(int totalFrames) {
        super(totalFrames);
    }

    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        int oldestAccess = Integer.MAX_VALUE;
        int victimIndex = -1;

        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (f.isOccupied() && f.getLastAccessTime() < oldestAccess) {
                oldestAccess = f.getLastAccessTime();
                victimIndex = i;
            }
        }

        Logger.memLog("[LRU]Se selecciono marco " + victimIndex + " como victima");

        return victimIndex;
    }

    @Override
    public String getAlgorithmName() {
        return "LRU (Least Recently Used)";
    }

    public String getAccessState() {
        StringBuilder sb = new StringBuilder("Estado de accesos LRU:\n");
        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            sb.append("Marco ").append(i).append(": ").append(f.toString())
              .append(" | lastAccess=").append(f.getLastAccessTime()).append("\n");
        }
        return sb.toString();
    }


    public void printAccessState() {
        Logger.memLog(getAccessState());
        // GUI: dibujar heatmap de lastAccessTime
    }
}