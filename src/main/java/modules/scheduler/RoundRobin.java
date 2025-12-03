package modules.scheduler;

import model.Process;
import utils.Logger;

/*
Round Robin : Asigna un quantum fijo a cada proceso y es un planificador apropiativo.

selectNextProcess():
 - Devuelve el siguiente proceso en la cola sin removerlo.

shouldPreempt():
 - Interrumpe cuando el quantum llega a cero.

decrementaQuantum():
 - Reduce el quantum en cada unidad de tiempo ejecutada.

isQuantumAgotado():
 - Indica si el quantum ya se agoto.

resetQuantum():
 - Restaura el quantum completo para un nuevo proceso.

confirmProcessSelection():
 - Remueve el proceso de la cola, reinicia su quantum y realiza el cambio de contexto.

getAlgorithmName():
 - Retorna el nombre del algoritmo con su quantum configurado.

*/

public class RoundRobin extends Scheduler {
    
    private final int quantum;
    private int currentQuantumRemaining;
    private Process currentProcessInExecution;

    public RoundRobin(int quantum) {
        super();
        
        if (quantum <= 0) {
            throw new IllegalArgumentException("El quantum debe ser mayor a 0");
        }
        
        this.quantum = quantum;
        this.currentQuantumRemaining = 0;
        
        Logger.exeLog("Planificador Round Robin inicializado (quantum=" + quantum + ")");
    }
    
    @Override
    public synchronized Process selectNextProcess() {
        Process next = readyQueue.peek();
        
        if (next != null) {
            // Resetear quantum completo al nuevo proceso (se asignará en confirmProcessSelection)
            Logger.procLog(String.format("RR selecciono: %s (quantum=%d)", 
                        next.getPid(), quantum));
        }
        
        return next;
    }
    
    @Override
    public boolean shouldPreempt(Process current, Process candidate) {
        // Round Robin debe interrumpir cuando el quantum se agota
        if (currentQuantumRemaining <= 0) {
            Logger.exeLog("Quantum agotado para proceso " + current.getPid());
            return true;
        }
        return false;
    }
    
    //Decrementa el quantum cuando se ejecuta una unidad de tiempo
    //Porsiaco Este método debe ser llamado por el SyncController después de cada ejecución
     
    public synchronized void decrementaQuantum() {
      System.out.println(">>> [DEBUG-RR-DECREMENT] Antes: quantum=" + currentQuantumRemaining);
      
      if (currentQuantumRemaining > 0) {
          currentQuantumRemaining--;
          System.out.println(">>> [DEBUG-RR-DECREMENT] Después: quantum=" + currentQuantumRemaining);
      } else {
          System.out.println(">>> [DEBUG-RR-DECREMENT] Ya está agotado (quantum=" + 
                             currentQuantumRemaining + ")");
      }
    }
    
    //Verifica si el quantum se agotó

    public synchronized boolean isQuantumAgotado() {
      boolean agotado = currentQuantumRemaining <= 0;
      System.out.println(">>> [DEBUG-RR-AGOTADO] isQuantumAgotado: " + agotado + 
                         " (quantum=" + currentQuantumRemaining + ")");
      return agotado;
    }
    
    //Reinicia el quantum para un nuevo proceso
    public synchronized void resetQuantum() {
      System.out.println(">>> [DEBUG-RR-RESET] Reseteando quantum: " + 
                         currentQuantumRemaining + " → " + quantum);
      currentQuantumRemaining = quantum;
    }
    
    // Override para resetear el quantum cuando se confirma la selección
    @Override
    public synchronized void confirmProcessSelection(Process process) {
        System.out.println(">>> [DEBUG-RR-CONFIRM] Confirmando selección de " + 
                           (process != null ? process.getPid() : "NULL"));
        System.out.println(">>>   Antes: currentQuantumRemaining=" + currentQuantumRemaining);
        
        if (process != null && readyQueue.remove(process)) {
            currentQuantumRemaining = quantum;
            currentProcessInExecution = process;
            
            System.out.println(">>> [DEBUG-RR-CONFIRM] Proceso removido de cola");
            System.out.println(">>> [DEBUG-RR-CONFIRM] Después: currentQuantumRemaining=" + 
                               currentQuantumRemaining);
            
            contextSwitch(process);
        } else {
            System.out.println(">>> [DEBUG-RR-CONFIRM] No se pudo remover de la cola");
        }
    }

    
    
    @Override
    public String getAlgorithmName() {
        return "Round Robin (quantum=" + quantum + ")";
    }
    
    public int getQuantum() {
        return quantum;
    }
    
    public synchronized int getQuantumActual() {
        return currentQuantumRemaining; 
    }
}