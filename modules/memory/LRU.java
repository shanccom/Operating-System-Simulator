package modules.memory;

import model.Process;
import utils.Logger;

/**
 * LRU (Least Recently Used)
 * Reemplaza la página menos recientemente usada.
 * 
 */
public class LRU extends MemoryManager {

    public LRU(int totalFrames) {
        super(totalFrames);
        Logger.log("LRU MemoryManager inicializado con " + totalFrames + " marcos");
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

        Logger.debug("LRU seleccionó marco " + victimIndex + " como víctima");

        // GUI: onVictimSelected(victimIndex, frames[victimIndex].getProcessId(), frames[victimIndex].getPageNumber(), "LRU", currentTime);
        return victimIndex;
    }

    @Override
    public String getAlgorithmName() {
        return "LRU (Least Recently Used)";
    }

    /**
     * Devuelve el estado de los marcos con sus tiempos de acceso.
     */
    public String getAccessState() {
        StringBuilder sb = new StringBuilder("Estado de accesos LRU:\n");
        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            sb.append("Marco ").append(i).append(": ").append(f.toString())
              .append(" | lastAccess=").append(f.getLastAccessTime()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Imprime el estado detallado de accesos.
     */
    public void printAccessState() {
        Logger.log(getAccessState());
        // GUI: dibujar heatmap de lastAccessTime
    }
}