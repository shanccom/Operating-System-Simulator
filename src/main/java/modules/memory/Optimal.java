package modules.memory;

import model.Process;
import utils.Logger;
import java.util.*;

public class Optimal extends MemoryManager {

    private final Map<String, List<Integer>> futureAccesses = new HashMap<>();
    private final Map<String, Integer> accessIndex = new HashMap<>(); // ❌ FALTABA ESTO

    public Optimal(int totalFrames) {
        super(totalFrames);
        Logger.memLog("Optimal MemoryManager inicializado con " + totalFrames + " marcos");
    }

    public void setFutureAccesses(String pid, List<Integer> accesses) {
        futureAccesses.put(pid, new ArrayList<>(accesses));
        accessIndex.put(pid, 0); 
    }

    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        String pid = requestingProcess.getPid();
        List<Integer> future = futureAccesses.getOrDefault(pid, Collections.emptyList());
        int currentIndex = accessIndex.getOrDefault(pid, 0); // ❌ FALTABA

        int farthest = -1;
        int victim = -1;

        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (!f.isOccupied()) continue;

            int page = f.getPageNumber();
            String framePid = f.getProcessId();
            
            // desde current Index
            int nextUse = findNextUse(framePid, page, currentIndex);

            if (nextUse > farthest) {
                farthest = nextUse;
                victim = i;
            }
        }

        Logger.memLog("Optimal seleccionó marco " + victim + " (distancia: " + farthest + ")");
        return victim;
    }

    //Metodo para buscar próximo uso
    private int findNextUse(String pid, int page, int fromIndex) {
        List<Integer> future = futureAccesses.getOrDefault(pid, Collections.emptyList());
        
        for (int i = fromIndex; i < future.size(); i++) {
            if (future.get(i) == page) {
                return i - fromIndex; // distancia relativa
            }
        }
        return Integer.MAX_VALUE; // nunca se usará
    }

    //actualizar índice cuando accedes una página
    @Override
    protected void accessPage(String pid, int pageNumber) {
        super.accessPage(pid, pageNumber);
        // Avanzar el índice de accesos futuros
        int idx = accessIndex.getOrDefault(pid, 0);
        accessIndex.put(pid, idx + 1);
    }

    @Override
    public String getAlgorithmName() {
        return "Optimal";
    }
}