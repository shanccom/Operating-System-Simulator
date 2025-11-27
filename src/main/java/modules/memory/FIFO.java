package modules.memory;

import model.Process;
import utils.Logger;

import java.util.LinkedList;
import java.util.Queue;


public class FIFO extends MemoryManager {


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

        
    }

    @Override
    protected void replacePage(int frameIndex, String newPid, int newPage) {
        // Antes de reemplazar, podemos loggear la víctima
        String oldPid = frames[frameIndex].getProcessId();
        int oldPage = frames[frameIndex].getPageNumber();
        Logger.debug("FIFO reemplazara pagina " + oldPage + " del proceso " + oldPid);

      

        // Llamar al método padre para reemplazar
        super.replacePage(frameIndex, newPid, newPage);

        // Reinsertar el marco en la cola (ahora es el mas reciente)
        frameQueue.remove(frameIndex);
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


    public String getQueueState() {
        return "Cola FIFO: " + frameQueue.toString();
    }


    public int getQueueSize() {
        return frameQueue.size();
    }


    public boolean isInQueue(int frameIndex) {
        return frameQueue.contains(frameIndex);
    }


    public Queue<Integer> getQueueSnapshot() {
        return new LinkedList<>(frameQueue);
    }


    public void printQueueState() {
        Logger.log("Estado actual de la cola FIFO:");
        for (Integer frameIndex : frameQueue) {
            Frame f = frames[frameIndex];
            Logger.log(" -> Marco " + frameIndex + ": " + f.toString());
        }
        
    }
}