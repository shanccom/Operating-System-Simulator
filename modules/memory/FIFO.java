package modules.memory;

import model.Process;
import utils.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * FIFO (First In First Out) - Algoritmo de reemplazo de páginas
 * Reemplaza la página que lleva más tiempo en memoria
 * 
 * ESTE ES UN EJEMPLO para qeu te guies
 */
public class FIFO extends MemoryManager {
    
    private final Queue<Integer> frameQueue;
    
    public FIFO(int totalFrames) {
        super(totalFrames);
        this.frameQueue = new LinkedList<>();
        Logger.log("Algoritmo FIFO inicializado con " + totalFrames + " marcos");
    }
    
    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        // FIFO: seleccionar el marco que fue cargado primero (más antiguo)
        
        if (frameQueue.isEmpty()) {
            Logger.warning("Cola FIFO vacía, no debería pasar");
            return 0; // Fallback
        }
        
        // El primer elemento de la cola es el marco más antiguo
        int victimFrame = frameQueue.poll();
        
        Logger.debug("FIFO seleccionó marco " + victimFrame + " como víctima");
        
        return victimFrame;
    }
    
    @Override
    protected void loadPageToFrame(int frameIndex, String pid, int pageNumber) {
        // Llamar al método padre para cargar la página
        super.loadPageToFrame(frameIndex, pid, pageNumber);
        
        // Agregar el marco a la cola FIFO
        frameQueue.offer(frameIndex);
        
        Logger.debug("Marco " + frameIndex + " agregado a cola FIFO");
    }
    
    @Override
    protected void replacePage(int frameIndex, String newPid, int newPage) {
        // Llamar al método padre para reemplazar
        super.replacePage(frameIndex, newPid, newPage);
        
        // Agregar nuevamente el marco a la cola (ahora es el más reciente)
        frameQueue.offer(frameIndex);
        
        Logger.debug("Marco " + frameIndex + " reinsertado en cola FIFO");
    }
    
    @Override
    public String getAlgorithmName() {
        return "FIFO (First In First Out)";
    }
    
    @Override
    public synchronized void reset() {
        super.reset();
        frameQueue.clear();
        Logger.log("Cola FIFO limpiada");
    }
    
    /**
     * Información de debugging
     */
    public String getQueueState() {
        return "Cola FIFO: " + frameQueue.toString();
    }
}