package modules.memory;

import model.Process;
import utils.Logger;

import java.util.*;

/**
 * Optimal
 * Reemplaza la página cuyo próximo uso está más lejos.
 * Requiere trazas futuras simuladas.
 * 
 * Mejorado con:
 * - Métodos auxiliares para mostrar distancias futuras.
 * - Comentarios para futura GUI.
 */
public class Optimal extends MemoryManager {

    private final Map<String, List<Integer>> futureAccesses = new HashMap<>();

    public Optimal(int totalFrames) {
        super(totalFrames);
        Logger.log("Optimal MemoryManager inicializado con " + totalFrames + " marcos");
    }

    // Configura accesos futuros para un proceso
    public void setFutureAccesses(String pid, List<Integer> accesses) {
        futureAccesses.put(pid, new ArrayList<>(accesses));
    }

    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        String pid = requestingProcess.getPid();
        List<Integer> future = futureAccesses.getOrDefault(pid, Collections.emptyList());

        int farthest = -1;
        int victim = -1;

        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (!f.isOccupied()) continue;

            int page = f.getPageNumber();
            int index = future.indexOf(page);
            int distance = (index == -1) ? Integer.MAX_VALUE : index;

            if (distance > farthest) {
                farthest = distance;
                victim = i;
            }
        }

        Logger.debug("Optimal seleccionó marco " + victim + " como víctima (distancia futura: " + farthest + ")");

        // GUI: onVictimSelected(victim, frames[victim].getProcessId(), frames[victim].getPageNumber(), "Optimal", currentTime);
        return victim;
    }

    @Override
    public String getAlgorithmName() {
        return "Optimal";
    }

    /**
     * Devuelve estado de distancias futuras.
     */
    public String getFutureState(String pid) {
        StringBuilder sb = new StringBuilder("Estado de distancias futuras Optimal:\n");
        List<Integer> future = futureAccesses.getOrDefault(pid, Collections.emptyList());
        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (f.isOccupied()) {
                int page = f.getPageNumber();
                int index = future.indexOf(page);
                int distance = (index == -1) ? Integer.MAX_VALUE : index;
                sb.append("Marco ").append(i).append(": ").append(f.toString())
                  .append(" | Distancia=").append(distance).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Imprime estado de distancias futuras.
     */
    public void printFutureState(String pid) {
        Logger.log(getFutureState(pid));
        // GUI: dibujar distancias al próximo uso
    }
}