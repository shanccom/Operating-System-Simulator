package modules.memory;

import model.Process;
import utils.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * FIFO (First In First Out) - Algoritmo de reemplazo de paginas
 * Reemplaza la pagina que lleva mas tiempo en memoria.
 */
public class FIFO extends MemoryManager {

    // Cola que mantiene el orden de llegada de los marcos
    private final Queue<Integer> frameQueue;

    public FIFO(int totalFrames) {
        super(totalFrames);
        this.frameQueue = new LinkedList<>();
        Logger.log("Algoritmo FIFO inicializado con " + totalFrames + " marcos");

        System.out.println("🚨 STACK TRACE - ¿Quién creó FIFO?");
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 0; i < Math.min(5, stack.length); i++) {
            System.out.println("  " + stack[i]);
        }
    }

    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        // FIFO: seleccionar el marco que fue cargado primero (mas antiguo)

        if (frameQueue.isEmpty()) {
            Logger.warning("Cola FIFO vacía, seleccionando primer marco ocupado como fallback");
            for (int i = 0; i < totalFrames; i++) {
                if (frames[i].isOccupied()) {
                    // GUI: aquí podrías resaltar el marco elegido como fallback
                    return i;
                }
            }
            return -1;
        }

        // El primer elemento de la cola es el marco mas antiguo
        int victimFrame = frameQueue.poll();

        Logger.debug("FIFO seleccionó marco " + victimFrame + " como víctima");

        // GUI: onVictimSelected(victimFrame, frames[victimFrame].getProcessId(), frames[victimFrame].getPageNumber(), "FIFO", currentTime);
        return victimFrame;
    }

    @Override
    protected void loadPageToFrame(int frameIndex, String pid, int pageNumber) {
        // Llamar al método padre para cargar la pagina
        super.loadPageToFrame(frameIndex, pid, pageNumber);

        // Evitar duplicados en la cola
        frameQueue.remove(frameIndex);
        frameQueue.offer(frameIndex);

        Logger.debug("Marco " + frameIndex + " agregado a cola FIFO");

        // GUI: onPageIn(frameIndex, pid, pageNumber, currentTime);
    }

    @Override
    protected void replacePage(int frameIndex, String newPid, int newPage) {
        // Antes de reemplazar, podemos loggear la víctima
        String oldPid = frames[frameIndex].getProcessId();
        int oldPage = frames[frameIndex].getPageNumber();
        Logger.debug("FIFO reemplazara pagina " + oldPage + " del proceso " + oldPid);

        // GUI: onPageOut(frameIndex, oldPid, oldPage, currentTime);

        // Llamar al método padre para reemplazar
        super.replacePage(frameIndex, newPid, newPage);

        // Reinsertar el marco en la cola (ahora es el mas reciente)
        frameQueue.remove(frameIndex);
        frameQueue.offer(frameIndex);

        Logger.debug("Marco " + frameIndex + " reinsertado en cola FIFO");

        // GUI: onPageIn(frameIndex, newPid, newPage, currentTime);
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
        // GUI: limpiar visualización de la cola
    }

    /**
     * Devuelve el estado actual de la cola FIFO.
     * Útil para debugging y para mostrar en GUI.
     */
    public String getQueueState() {
        return "Cola FIFO: " + frameQueue.toString();
    }

    /**
     * Devuelve el tamaño actual de la cola FIFO.
     */
    public int getQueueSize() {
        return frameQueue.size();
    }

    /**
     * Devuelve true si el marco esta en la cola FIFO.
     */
    public boolean isInQueue(int frameIndex) {
        return frameQueue.contains(frameIndex);
    }

    /**
     * Devuelve una copia de la cola FIFO (para GUI o debugging).
     */
    public Queue<Integer> getQueueSnapshot() {
        return new LinkedList<>(frameQueue);
    }

    /**
     * Imprime el estado detallado de la cola FIFO.
     */
    public void printQueueState() {
        Logger.log("Estado actual de la cola FIFO:");
        for (Integer frameIndex : frameQueue) {
            Frame f = frames[frameIndex];
            Logger.log(" -> Marco " + frameIndex + ": " + f.toString());
        }
        // GUI: aquí podrías dibujar la cola completa en pantalla
    }
}