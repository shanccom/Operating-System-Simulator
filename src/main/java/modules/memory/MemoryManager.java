package modules.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
<<<<<<< HEAD
// Clase base para gestion de memoria virtual
public abstract class MemoryManager {
  
  protected static class Frame { //Desripcion diagrama de clases
      private String processId; //ownerPID
      private int pageNumber; 
      private int loadTime;
      private int lastAccessTime;
      private boolean isOccupied;
      
      public Frame() {
          this.isOccupied = false;
      }
      
      public void load(String pid, int page, int time) {
          this.processId = pid;
          this.pageNumber = page;
          this.loadTime = time;
          this.lastAccessTime = time;
          this.isOccupied = true;
      }
      
      public void unload() {
          this.processId = null;
          this.pageNumber = -1;
          this.isOccupied = false;
      }
      
      public void access(int time) {
          this.lastAccessTime = time;
      }
      //Falta clear, isFree, touch
      // Getters
      public String getProcessId() { return processId; }
      public int getPageNumber() { return pageNumber; }
      public int getLoadTime() { return loadTime; }
      public int getLastAccessTime() { return lastAccessTime; }
      public boolean isOccupied() { return isOccupied; }
      
      @Override
      public String toString() {
          return isOccupied ? 
              String.format("[%s:P%d]", processId, pageNumber) : "[FREE]";
      }
  }
  
  protected final int totalFrames;
  protected final Frame[] frames;
  protected final Map<String, Set<Integer>> processPageMap; // PID Paginas cargadas
  protected final Map<String, Integer> reemplazosPorProceso;
  
  protected int currentTime;
  protected int pageFaults;
  protected int pageReplacements;
  protected int totalPageLoads;
  
  public MemoryManager(int totalFrames) {
      if (totalFrames <= 0) {
          throw new IllegalArgumentException("[MEM] Numero de marcos debe ser positivo");
      }
      
      this.totalFrames = totalFrames;
      this.frames = new Frame[totalFrames];
      this.processPageMap = new ConcurrentHashMap<>();
      this.reemplazosPorProceso = new ConcurrentHashMap<>();
      
      for (int i = 0; i < totalFrames; i++) {
          frames[i] = new Frame();
      }
      this.currentTime = 0;
      this.pageFaults = 0;
      this.pageReplacements = 0;
      this.totalPageLoads = 0;
  }
  
  // Intenta cargar una pagina en memoria
  public synchronized boolean loadPage(Process process, int pageNumber) {
      currentTime++;
      
      String pid = process.getPid();
      
      // Verificar si la pagina ya esta cargada, no se proudcira page fault
      if (isPageLoaded(pid, pageNumber)) {
          Logger.debug("[MEM] Pagina " + pageNumber + " del proceso " + pid + " ya esta en memoria");
          accessPage(pid, pageNumber);
          return true;
      }
      
      // Page fault
      pageFaults++;
      process.incrementPageFaults();
      Logger.logPageFault(pid, pageNumber, currentTime);
      
      // Buscar marco libre
      int freeFrame = findFreeFrame();
      
      if (freeFrame != -1) {
          // Hay marco libre
          loadPageToFrame(freeFrame, pid, pageNumber);
          return true;
      }
      
      // No hay marco libre, necesitamos reemplazar
      int victimFrame = selectVictimFrame(process, pageNumber);
      
      if (victimFrame != -1) {
          replacePage(victimFrame, pid, pageNumber);
          return true;
      }
      //Para debugguear
      Logger.error("[MEM] No se pudo cargar la pagina " + pageNumber + " del proceso " + pid);
      return false;
  }
  
  // Selecciona el marco victima para reemplazo
  protected abstract int selectVictimFrame(Process requestingProcess, int requestedPage);
  
  protected void accessPage(String pid, int pageNumber) {
      for (int i = 0; i < totalFrames; i++) {
          Frame frame = frames[i];
          if (frame.isOccupied() && 
              frame.getProcessId().equals(pid) && 
              frame.getPageNumber() == pageNumber) {
              frame.access(currentTime);
              break;
          }
      }
  }
  
  // Busca un marco libre
  protected int findFreeFrame() {
      for (int i = 0; i < totalFrames; i++) {
          if (!frames[i].isOccupied()) {
              return i;
          }
      }
      return -1;
  }
  
  // Carga una pagina en un marco especifico
  protected void loadPageToFrame(int frameIndex, String pid, int pageNumber) {
      frames[frameIndex].load(pid, pageNumber, currentTime);
      
      processPageMap.computeIfAbsent(pid, k -> new HashSet<>()).add(pageNumber);
      
      totalPageLoads++;
      Logger.debug("[MEM] Pagina " + pageNumber + " del proceso " + pid + 
                  " cargada en marco " + frameIndex);
  }
  
  // Reemplaza una pagina en un marco
  protected void replacePage(int frameIndex, String newPid, int newPage) {
      Frame frame = frames[frameIndex];
      String victimPid = frame.getProcessId();
      int victimPage = frame.getPageNumber();
      
      // Remover la pagina victima
      if (processPageMap.containsKey(victimPid)) {
          processPageMap.get(victimPid).remove(victimPage);
      }
      
      // Cargar la nueva pagina
      frame.load(newPid, newPage, currentTime);
      processPageMap.computeIfAbsent(newPid, k -> new HashSet<>()).add(newPage);

      pageReplacements++;
      totalPageLoads++;
      reemplazosPorProceso.merge(newPid, 1, Integer::sum);
      
      Logger.logPageReplacement(victimPid, victimPage, newPid, newPage, currentTime);
  }
  
  // Verifica si una pagina esta cargada
  public synchronized boolean isPageLoaded(String pid, int pageNumber) {
      return processPageMap.containsKey(pid) && 
              processPageMap.get(pid).contains(pageNumber);
  }
  
  // Obtiene todas las paginas cargadas de un proceso
  public synchronized Set<Integer> getLoadedPages(String pid) {
      return new HashSet<>(processPageMap.getOrDefault(pid, new HashSet<>()));
  }
  
  // Libera todas las paginas de un proceso
  public synchronized void freeProcessPages(String pid) {
      for (int i = 0; i < totalFrames; i++) {
          if (frames[i].isOccupied() && frames[i].getProcessId().equals(pid)) {
              frames[i].unload();
          }
      }
      processPageMap.remove(pid);
      Logger.debug("[MEM] Paginas del proceso " + pid + " liberadas");
  }
  
  // Obtiene el estado actual de los marcos
  public synchronized String getMemoryState() {
      StringBuilder sb = new StringBuilder();
      sb.append("Estado de Memoria:\n");
      for (int i = 0; i < totalFrames; i++) {
          sb.append(String.format("[MEM] Marco %2d: %s\n", i, frames[i]));
      }
      return sb.toString();
  }
  
  // Obtiene marcos libres
  public synchronized int getFreeFrames() {
      int count = 0;
      for (Frame frame : frames) {
          if (!frame.isOccupied()) count++;
      }
      return count;
  }
  
  // Getters para metricas
  public int getPageFaults() {
      return pageFaults;
  }
  
  public int getPageReplacements() {
      return pageReplacements;
  }

  //Para la pagina de Resultados
  public Map<String, Integer> getReemplazosPorProceso() {
      return new HashMap<>(reemplazosPorProceso);
  }
  
  public int getTotalPageLoads() {
      return totalPageLoads;
  }
  
  public int getTotalFrames() {
      return totalFrames;
  }
  
  public int getCurrentTime() {
      return currentTime;
  }
  
  public void setCurrentTime(int time) {
      this.currentTime = time;
  }
  
  public abstract String getAlgorithmName();
  

  public void printMetrics() {
      System.out.println();
      Logger.log("[MEM] METRICAS DE MEMORIA - " + getAlgorithmName());
      Logger.log("Total de fallos de pagina: " + pageFaults);
      Logger.log("Total de reemplazos: " + pageReplacements);
      Logger.log("Total de cargas de pagina: " + totalPageLoads);
      Logger.log("Marcos libres: " + getFreeFrames() + "/" + totalFrames);
      if (totalPageLoads > 0) {
          Logger.log("Tasa de fallos: " +
              String.format("%.2f%%", (double) pageFaults / totalPageLoads * 100));
      } else {
          Logger.log("Tasa de fallos: N/A");
      }
  }

  public synchronized void reset() {
      for (Frame frame : frames) {
          frame.unload();
      }
      processPageMap.clear();
      currentTime = 0;
      pageFaults = 0;
      pageReplacements = 0;
      totalPageLoads = 0;
      reemplazosPorProceso.clear();
      Logger.log("[MEM] Memoria reseteada");
  }
=======

import model.Process;
import utils.Logger;
import modules.memory.MemoryEventListener;

// NUEVO ADECUAMOS PARA USAR DISPARADORES de eventos-aun
// NUEVO SI - Atributos necesarios para realizar NRU y optimal
// Clase base para gestion de memoria virtual
public abstract class MemoryManager {

    // FRAME ____________________________________________________________________________
    public static class Frame { // CADA FRAME
        private String processId;  // ownerPID
        private int pageNumber;    // numero de pagina cargada
        private int loadTime;
        private int lastAccessTime; // Para NRU y LRU
        private boolean referenced; // BIT R
        private boolean modified;   // BIT M
        private boolean isOccupied;

        public Frame() {
            this.isOccupied = false;
            this.referenced = false;
            this.modified = false;
        }

        public void load(String pid, int page, int time) {
            this.processId = pid;
            this.pageNumber = page;
            this.loadTime = time;
            this.lastAccessTime = time;
            this.isOccupied = true;
            this.referenced = true;
            this.modified = false;
        }

        public void unload() {
            this.processId = null;
            this.pageNumber = -1;
            this.isOccupied = false;
            this.referenced = false;
            this.modified = false;
        }

        public void access(int time) {
            this.lastAccessTime = time;
            this.referenced = true;
        }

        // Metodos adicionales para NRU
        public void markRead() { this.referenced = true; }
        public void markWrite() { this.modified = true; this.referenced = true; }
        public void resetReferenced() { this.referenced = false; }

        // Getters
        public String getProcessId() { return processId; }
        public int getPageNumber() { return pageNumber; }
        public int getLoadTime() { return loadTime; }
        public int getLastAccessTime() { return lastAccessTime; }
        public boolean isOccupied() { return isOccupied; }
        public boolean isReferenced() { return referenced; }
        public boolean isModified() { return modified; }

        @Override
        public String toString() {
            return isOccupied ?
                    String.format("[%s:P%d R=%b M=%b]", processId, pageNumber, referenced, modified)
                    : "[FREE]";
        }
    }
    // FIN FRAME ________________________________________________________________________

    private List<MemoryEventListener> listeners = new ArrayList<>();
    protected final int totalFrames;
    protected final Frame[] frames;
    protected final Map<String, Set<Integer>> processPageMap; // PID -> paginas cargadas

    protected int currentTime;
    protected int pageFaults;
    protected int pageReplacements;
    protected int totalPageLoads;

    public MemoryManager(int totalFrames) {
        if (totalFrames <= 0) {
            throw new IllegalArgumentException("[MEM] Numero de marcos debe ser positivo");
        }

        this.totalFrames = totalFrames;
        this.frames = new Frame[totalFrames];
        this.processPageMap = new ConcurrentHashMap<>();

        for (int i = 0; i < totalFrames; i++) {
            frames[i] = new Frame();
        }
        this.currentTime = 0;
        this.pageFaults = 0;
        this.pageReplacements = 0;
        this.totalPageLoads = 0;
    }

    //Listeners encapsulación
    public void addListener(MemoryEventListener listener){
        listeners.add(listener);
    }
    private void notifyPageFault(String pid, int page) {
        for (MemoryEventListener listener : listeners) {
            listener.onPageFault(pid, page);
        }
    }
        
    private void notifyPageAccess(int frameIndex, String pid, int page, boolean hit) {
        for (MemoryEventListener l : listeners)
            l.onPageAccess(frameIndex, pid, page, hit);
    }


    private void notifyFrameLoaded(int frameIndex, String pid, int page) {
        for (MemoryEventListener l : listeners)
            l.onFrameLoaded(frameIndex, pid, page);
    }

    private void notifyFrameEvicted(int frameIndex, String pid, int page) {
        for (MemoryEventListener l : listeners)
            l.onFrameEvicted(frameIndex, pid, page);
    }

    private void notifyVictimChosen(int frameIndex, String reason) {
        for (MemoryEventListener l : listeners)
            l.onVictimChosen(frameIndex, reason);
    }

    private void notifySnapshot(String snapshot) {
        for (MemoryEventListener l : listeners)
            l.onSnapshot(snapshot);
    }

    //load
    public synchronized boolean loadPage(Process process, int pageNumber) {
        //Listener
        currentTime++;
        String pid = process.getPid();

        // Caso: la pagina YA esta en memoria (HIT)
        
        if (isPageLoaded(pid, pageNumber)) {
            int frameIndex = findFrame(pid, pageNumber);
            Logger.memHit(pid, pageNumber, frameIndex);
            notifyPageAccess(frameIndex, pid, pageNumber, true);
            accessPage(pid, pageNumber);
            notifySnapshot(getMemorySnapshotCompact());
            Logger.memSnapshot(frames);
            return true;
        }


        // PAGE FAULT
        pageFaults++;
        process.incrementPageFaults();
        Logger.memFault(pid, pageNumber);
        notifyPageFault(pid, pageNumber);

        // Intentar cargar en frame libre
        int freeFrame = findFreeFrame();
        if (freeFrame != -1) {
            loadPageToFrame(freeFrame, pid, pageNumber);
            notifyFrameLoaded(freeFrame, pid, pageNumber);
            return true;
        }

        // Si no hay marcos libres → elegir víctima
        int victimFrame = selectVictimFrame(process, pageNumber);
        notifyVictimChosen(victimFrame, "Aca incluir reason");
        if (victimFrame != -1) {
            replacePage(victimFrame, pid, pageNumber);
            notifyFrameEvicted(victimFrame, pid, pageNumber);
            return true;
        }

        Logger.memLog("[MEM] ERROR: No se pudo cargar la pagina " + pageNumber + " del proceso " + pid);
        return false;
    }


    // Metodo añadido para obtener el frame real donde esta una pagina
    private int findFrame(String pid, int page) {
        for (int i = 0; i < totalFrames; i++) {
            if (frames[i].isOccupied()
                    && frames[i].getProcessId().equals(pid)
                    && frames[i].getPageNumber() == page) {
                return i;
            }
        }
        return -1;
    }


    protected abstract int selectVictimFrame(Process requestingProcess, int requestedPage);

    //Metodo auxiliares para los de arriba
    protected void accessPage(String pid, int pageNumber) {
        for (int i = 0; i < totalFrames; i++) {
            Frame frame = frames[i];
            if (frame.isOccupied()
                    && frame.getProcessId().equals(pid)
                    && frame.getPageNumber() == pageNumber) {
                frame.access(currentTime);
                return;
            }
        }
    }


    protected int findFreeFrame() { //Auxiliar
        for (int i = 0; i < totalFrames; i++) {
            if (!frames[i].isOccupied()) {
                return i;
            }
        }
        return -1;
    }


    protected void loadPageToFrame(int frameIndex, String pid, int pageNumber) {
        frames[frameIndex].load(pid, pageNumber, currentTime);
        processPageMap.computeIfAbsent(pid, k -> new HashSet<>()).add(pageNumber);
        totalPageLoads++;

        Logger.memLoad(pid, pageNumber, frameIndex);
        Logger.memSnapshot(frames);
    }


    protected void replacePage(int frameIndex, String newPid, int newPage) {

        Frame old = frames[frameIndex];
        String oldPid = old.getProcessId();
        int oldPage = old.getPageNumber();

        if (processPageMap.containsKey(oldPid)) {
            processPageMap.get(oldPid).remove(oldPage);
        }

        old.load(newPid, newPage, currentTime);
        processPageMap.computeIfAbsent(newPid, k -> new HashSet<>()).add(newPage);

        pageReplacements++;
        totalPageLoads++;

        Logger.memReplace(oldPid, oldPage, newPid, newPage, frameIndex, "Algoritmo: " + getAlgorithmName());
        Logger.memSnapshot(frames);
    }


    public synchronized boolean isPageLoaded(String pid, int pageNumber) {
        return processPageMap.containsKey(pid)
                && processPageMap.get(pid).contains(pageNumber);
    }


    public synchronized Set<Integer> getLoadedPages(String pid) {
        return new HashSet<>(processPageMap.getOrDefault(pid, new HashSet<>()));
    }


    public synchronized void freeProcessPages(String pid) {
        for (int i = 0; i < totalFrames; i++) {
            if (frames[i].isOccupied()
                    && frames[i].getProcessId().equals(pid)) {
                frames[i].unload();
            }
        }
        processPageMap.remove(pid);
        Logger.memLog("[MEM] Paginas del proceso " + pid + " liberadas");
        Logger.memSnapshot(frames);
    }


    public synchronized String getMemorySnapshotCompact() {
        StringBuilder sb = new StringBuilder();
        sb.append("MEMORIA FISICA ================================================================================\n");
        for (int i = 0; i < totalFrames; i++) {
            if (frames[i].isOccupied()) {
                sb.append(String.format("\tFrame%d=[%s:P%d]\n",
                        i, frames[i].getProcessId(), frames[i].getPageNumber()));
            } else {
                sb.append(String.format("\tFrame%d=[FREE]\n", i));
            }
        }
        sb.append("=================================================================================================\n");

        sb.append("Paginas de Proceso: ");
        for (Map.Entry<String, Set<Integer>> e : processPageMap.entrySet()) {
            Set<Integer> pages = new TreeSet<>(e.getValue());
            sb.append(String.format("%s=%s; ", e.getKey(), pages));
        }
        return sb.toString();
    }


    public synchronized int getFreeFrames() {
        int count = 0;
        for (Frame frame : frames) {
            if (!frame.isOccupied()) count++;
        }
        return count;
    }

    // MeTRICAS
    public int getPageFaults() { return pageFaults; }
    public int getPageReplacements() { return pageReplacements; }
    public int getTotalPageLoads() { return totalPageLoads; }
    public int getTotalFrames() { return totalFrames; }
    public int getCurrentTime() { return currentTime; }

    public void setCurrentTime(int time) {
        this.currentTime = time;
    }

    public abstract String getAlgorithmName();


    public void printMetrics() {
        Logger.memLog("[MEM] METRICAS - " + getAlgorithmName());
        Logger.memLog("Fallos de pagina: " + pageFaults);
        Logger.memLog("Reemplazos: " + pageReplacements);
        Logger.memLog("Cargas totales: " + totalPageLoads);
        Logger.memLog("Marcos libres: " + getFreeFrames() + "/" + totalFrames);
    }


    public synchronized void reset() {
        for (Frame frame : frames) {
            frame.unload();
        }
        processPageMap.clear();
        currentTime = 0;
        pageFaults = 0;
        pageReplacements = 0;
        totalPageLoads = 0;
        Logger.memLog("[MEM] Memoria reseteada");
    }
>>>>>>> 410deea9e8d06a852e3ec72473d855e43c0db890
}