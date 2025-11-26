package modules.memory;

import model.Process;
import utils.Logger;

import java.util.*;

/**
 * NRU (Not Recently Used)
 * Clasifica páginas en 4 clases según acceso/modificación.
 * Aquí se simula con aleatoriedad para que funcione sin bits R/M reales.
 */
public class NRU extends MemoryManager {

    public NRU(int totalFrames) {
        super(totalFrames);
        Logger.log("NRU MemoryManager inicializado con " + totalFrames + " marcos");
    }

    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        List<Integer> candidates = new ArrayList<>();

        for (int i = 0; i < totalFrames; i++) {
            if (frames[i].isOccupied()) {
                candidates.add(i);
            }
        }

        if (candidates.isEmpty()) return -1;

        int victim = candidates.get(new Random().nextInt(candidates.size()));
        Logger.debug("NRU seleccionó marco " + victim + " como víctima");

        // GUI: onVictimSelected(victim, frames[victim].getProcessId(), frames[victim].getPageNumber(), "NRU", currentTime);
        return victim;
    }

    @Override
    public String getAlgorithmName() {
        return "NRU (Not Recently Used)";
    }

    /**
     * Devuelve un estado simulado de clases NRU.
     */
    public String getClassState() {
        StringBuilder sb = new StringBuilder("Estado de clases NRU:\n");
        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (f.isOccupied()) {
                int simulatedClass = new Random().nextInt(4); // 0..3
                sb.append("Marco ").append(i).append(": ").append(f.toString())
                  .append(" | Clase=").append(simulatedClass).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Imprime estado simulado de clases NRU.
     */
    public void printClassState() {
        Logger.log(getClassState());
        // GUI: dibujar badges R/M y clases NRU
    }
}