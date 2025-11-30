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
    }

    @Override
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        // FIFO: seleccionar el marco que fue cargado primero (mas antiguo)

        if (frameQueue.isEmpty()) {
          Logger.warning("Cola FIFO vacía, no hay víctimas disponibles");
          return -1;
        }

        for (Integer frameIndex : frameQueue) {
          Frame frame = frames[frameIndex];
          // Puedes descomentar esto si NO quieres reemplazar páginas del mismo proceso:
          // if (!frame.getProcessId().equals(requestingProcess.getPid())) {
          //     return frameIndex;
          // }
          
          // O simplemente retornar el más antiguo:
          return frameIndex; // No hagas poll() aquí
        }
    
      return -1;
    }

    @Override
    protected void loadPageToFrame(int frameIndex, String pid, int pageNumber) {
      super.loadPageToFrame(frameIndex, pid, pageNumber);
      
      if (!frameQueue.contains(frameIndex)) {
          frameQueue.offer(frameIndex);
          Logger.debug("Marco " + frameIndex + " agregado a cola FIFO (nuevo)");
      }
    }

    @Override
    protected void replacePage(int frameIndex, String newPid, int newPage) {
      super.replacePage(frameIndex, newPid, newPage);
  
      if (!frameQueue.remove(frameIndex)) {
          Logger.warning("Marco " + frameIndex + " no estaba en cola FIFO");
      }
      frameQueue.offer(frameIndex);
       
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