
import model.Burst;
import model.Burst.BurstType;
import model.Process;
import model.ProcessState;
import modules.scheduler.FCFS;
import modules.scheduler.SJF;
import modules.scheduler.RoundRobin;
import modules.scheduler.SRT;
import modules.scheduler.Priority;
import modules.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**Clase de prueba INDEPENDIENTE para el módulo de planificación 
 * No requiere memoria, GUI, ni otros módulos
 *  javac TestScheduler.java model/*.java modules/scheduler/*.java
 * java TestScheduler
 */
public class TestScheduler2 {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║   PRUEBA DEL MÓDULO DE PLANIFICACIÓN                 ║");
        System.out.println("║   Testing FCFS, SJF, Round Robin                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
        
        // Crear procesos de prueba
        List<Process> testProcesses = createTestProcesses();
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PROCESOS DE PRUEBA");
        System.out.println("═══════════════════════════════════════════════════════");
        printProcessTable(testProcesses);
        
        //Probar cada algoritmo
        /*System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 1: FCFS (First Come First Served)            ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new FCFS(), cloneProcesses(testProcesses));
        
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 2: SJF (Shortest Job First)                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new SJF(), cloneProcesses(testProcesses));
        
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 3: Round Robin (quantum=2)                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new RoundRobin(2), cloneProcesses(testProcesses));
        
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 4: Round Robin (quantum=4)                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new RoundRobin(4), cloneProcesses(testProcesses));
        */
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 5: SRTF (Shortest Remaining Time First)      ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new SRT(), cloneProcesses(testProcesses));
        
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 6: Priority (No Preemptive)                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new Priority(false), cloneProcesses(testProcesses));

        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 7: Priority (Preemptive)                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new Priority(true), cloneProcesses(testProcesses));



        // Comparación final
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ COMPARACIÓN DE ALGORITMOS                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        compareAlgorithms(testProcesses);
    }
    
    /**
     * Crea procesos de prueba
     * P1: Llega en t=0, CPU(4), IO(3), CPU(5) 
     * P2: Llega en t=2, CPU(6), IO(2), CPU(3)
     * P3: Llega en t=4, CPU(3)
     */
    private static List<Process> createTestProcesses() {
        List<Process> processes = new ArrayList<>();
        
        // Proceso P1
        processes.add(new Process(
            "P1",
            0,
            Arrays.asList(
                new Burst(BurstType.CPU, 4),
                new Burst(BurstType.IO, 3),
                new Burst(BurstType.CPU, 5)
            ),
            1,
            4
        ));
        
        // Proceso P2
        processes.add(new Process(
            "P2",
            2,
            Arrays.asList(
                new Burst(BurstType.CPU, 1),
                new Burst(BurstType.IO, 2),
                new Burst(BurstType.CPU, 3)
            ),
            2,
            5
        ));
        
        // Proceso P3
        processes.add(new Process(
            "P3",
            4,
            Arrays.asList(
                new Burst(BurstType.CPU, 3)
            ),
            3,
            6
        ));
        
        return processes;
    }
    
    /**
     * Prueba un algoritmo de planificación con simulación completa
     */
    private static void testAlgorithm(Scheduler scheduler, List<Process> processes) {
        System.out.println("\n🔹 Algoritmo: " + scheduler.getAlgorithmName());
        System.out.println("─".repeat(60));

        int currentTime = 0;
        int maxTime = 100; // Límite de seguridad
        // Cola de procesos bloqueados en E/S

        Queue<IOBlock> ioQueue = new LinkedList<>();
        // Proceso actual en CPU

        Process currentProcess = null;
        Process preemptedProcess = null;

        // Agregar procesos que llegan al tiempo 0
        for (Process p : processes) {
            if (p.getArrivalTime() == 0) {
                //p.setState(ProcessState.READY);
                p.setState(ProcessState.READY);
                scheduler.addProcess(p);
                System.out.println("[t=" + currentTime + "] " + p.getPid() + " → READY");
            }
        }

        // Simulación
   // ← proceso expulsado por quantum

        while (currentTime < maxTime) {

            // 1. Verificar nuevas llegadas
            for (Process p : processes) {
                if (p.getArrivalTime() == currentTime && p.getState() == ProcessState.NEW) {
                    p.setState(ProcessState.READY);
                    scheduler.addProcess(p);
                    System.out.println("[t=" + currentTime + "] " + p.getPid() + " → READY");
                    
                    //VERIFICAR EXPROPIACIÓN POR LLEGADA 
                    if (currentProcess != null && scheduler.shouldPreempt(currentProcess, p)) {
                        System.out.println("[t=" + currentTime + "] " + 
                                         currentProcess.getPid() + 
                                         " → PREEMPTED (nueva llegada de " + p.getPid() + ")");
                        preemptedProcess = currentProcess;
                        preemptedProcess.setState(ProcessState.NEW);
                        currentProcess = null;
                    }
                }
            }
            
            
            

            // 3. Re-encolar proceso expulsado (si existe)

            //Aquí se agrega el preemptedProcess (si viene de la iteración anterior)
            if (preemptedProcess != null) {
                scheduler.addProcess(preemptedProcess);
                preemptedProcess = null;
            }

            // 2. Procesar E/S completadas

            List<IOBlock> completedIO = new ArrayList<>();

            for (IOBlock io : ioQueue) {
                //System.out.println("[t=" + currentTime + "] E/S de " + io.process.getPid() +  " restante: " + io.remainingTime);
                io.remainingTime--;
                if (io.remainingTime <= -1) {//le cambie a -1 para que dure en e/s y de ahir recien suba en el siguiente 

                    completedIO.add(io);
                    System.out.println("[t=" + currentTime + "] " + io.process.getPid() + 
                                     " completó E/S → READY");
                    io.process.advanceBurst(); // Avanzar a la siguiente ráfaga
                    io.process.setState(ProcessState.NEW);
                    scheduler.addProcess(io.process);

                    // *** VERIFICAR EXPROPIACIÓN POR E/S COMPLETADA ***
                    if (currentProcess != null && 
                        scheduler.shouldPreempt(currentProcess, io.process)) {
                        System.out.println("[t=" + currentTime + "] " + 
                                         currentProcess.getPid() + 
                                         " → PREEMPTED (retorno de E/S de " + 
                                         io.process.getPid() + ")");
                        preemptedProcess = currentProcess;
                        preemptedProcess.setState(ProcessState.NEW);
                        currentProcess = null;
                    }

                }
            }
            ioQueue.removeAll(completedIO);

            // 4. Actualizar tiempos de espera
            for (Process p : scheduler.getReadyQueueSnapshot()) {
                //System.out.println("[t=" + currentTime + "] " + p.getPid() + " espera en READY---------/n");
                p.incrementWaitingTime();
                //p.updateWaitingTime(currentTime);
            }

            // 5. Seleccionar proceso si CPU está libre
            if (currentProcess == null || currentProcess.getState() != ProcessState.RUNNING) {
                
                currentProcess = scheduler.selectNextProcess();
                if (currentProcess != null) {
                    currentProcess.setState(ProcessState.RUNNING);
                    currentProcess.markFirstExecution(currentTime);
                }
            }

            // Si no hay proceso actual, avanzar tiempo
            if (currentProcess == null) {

                // Verificar si realmente terminamos
                boolean allTerminated = allProcessesFinished(processes);
                boolean noIO = ioQueue.isEmpty();
                boolean noReady = !scheduler.hasReadyProcesses();

                if (allTerminated && noIO && noReady) {
                    System.out.println("[t=" + currentTime + "] ✓ Simulación completada exitosamente");
                    System.out.println("    - Todos los procesos terminados");
                    System.out.println("    - No hay procesos en E/S");
                    System.out.println("    - Cola READY vacía");
                    break;
                }
                // CPU ociosa pero hay trabajo pendiente
                scheduler.recordIdleTime(1);
                if (!ioQueue.isEmpty()) {
                    System.out.printf("[t=%d] CPU IDLE (esperando %d proceso(s) en E/S)\n", 
                                    currentTime, ioQueue.size());
                } else {
                    System.out.println("[t=" + currentTime + "] CPU IDLE");
                }
                currentTime++;
                scheduler.setCurrentTime(currentTime);
                continue;
            }

            // 6. Ejecutar ráfaga de CPU
            Burst burst = currentProcess.getCurrentBurst();

            if (burst != null && burst.isCPU()) {
                // Ejecutando rafaga de cpu

                boolean burstCompleted = burst.execute(1);
                scheduler.recordCPUTime(1);

                System.out.printf("[t=%d] %s ejecutando %s (restante: %d)\n",
                    currentTime, currentProcess.getPid(), burst.getType(),
                    burst.getRemainingTime());

                // 7. Verificar si terminó la ráfaga
                if (burstCompleted) {
                    System.out.println("[t=" + currentTime + "] "
                            + currentProcess.getPid() + " completó ráfaga CPU");

                    currentProcess.advanceBurst();

                    if (currentProcess.isCompleted()) {
                        currentProcess.setState(ProcessState.TERMINATED);
                        currentProcess.setCompletionTime(currentTime + 1);
                        scheduler.onProcessComplete(currentProcess);
                        System.out.println("[t=" + currentTime + "] "
                                + currentProcess.getPid() + " → TERMINATED");
                        currentProcess = null;
                    } else {
                        // Verificar siguiente ráfaga

                        Burst nextBurst = currentProcess.getCurrentBurst();
                        if (nextBurst != null && nextBurst.isIO()) {
                            // Siguiente ráfaga es E/S
                            currentProcess.setState(ProcessState.BLOCKED_IO);
                            System.out.println("[t=" + currentTime + "] " + 
                                             currentProcess.getPid() + " → BLOCKED_IO (duración: " + 
                                             nextBurst.getDuration() + ")");

                            // Agregar a la cola de E/S
                            ioQueue.add(new IOBlock(currentProcess, nextBurst.getDuration()));
                            currentProcess = null; // Liberar CPU
                        } else {
                            // Siguiente ráfaga es CPU, devolver a READY
                            currentProcess.setState(ProcessState.NEW);
                            preemptedProcess = currentProcess;
                            currentProcess = null;

                        }

                    }

                } else {
                    // Ráfaga de CPU NO completada

                    // *** ROUND ROBIN: verificar quantum ***
                    if (scheduler instanceof RoundRobin) {
                        RoundRobin rr = (RoundRobin) scheduler;
                        rr.decrementaQuantum();
                        if (rr.isQuantumAgotado()) {
                            System.out.println("[t=" + currentTime + "] " + currentProcess.getPid() + " → PREEMPTED (quantum)");

                            // Guardamos para reinsertar en el siguiente tick (DESPUÉS de las llegadas)
                            preemptedProcess = currentProcess;
                            preemptedProcess.setState(ProcessState.NEW);
                            currentProcess = null;
                        }
                    }

                    /*  *** VERIFICAR EXPROPIACIÓN POR QUANTUM (PRIORITY + RR) ***
                    if (scheduler instanceof PriorityRR) {
                        PriorityRR prr = (PriorityRR) scheduler;
                        prr.decrementaQuantum();
                        if (prr.isQuantumAgotado()) {
                            System.out.println("[t=" + currentTime + "] " + 
                                             currentProcess.getPid() + 
                                             " → PREEMPTED (quantum agotado)");
                            preemptedProcess = currentProcess;
                            preemptedProcess.setState(ProcessState.NEW);
                            currentProcess = null;
                        }
                    }
                    */
                    
                    

                }
            }

            currentTime++;
            scheduler.setCurrentTime(currentTime);
        }
        if (currentTime >= maxTime) {

            System.out.println("\nSimulación detenida por límite de tiempo");

        }

        // Métricas
        System.out.println("\n" + "─".repeat(60));
        scheduler.printMetrics();

        System.out.println("\nDetalle por proceso:");
        System.out.printf("%-6s %-10s %-12s %-12s\n", "PID", "Waiting", "Turnaround", "Response");
        System.out.println("─".repeat(45));
        for (Process p : processes) {
            System.out.printf("%-6s %-10d %-12d %-12d\n",
                p.getPid(),
                p.getWaitingTime(),
                p.getTurnaroundTime(),
                p.getResponseTime()
            );
        }
    }


     /**
     * Cuenta procesos activos (no terminados)
     */
    private static int countActiveProcesses(List<Process> processes) {
        return (int) processes.stream()
            .filter(p -> p.getState() != ProcessState.TERMINATED)
            .count();
    }
    
    /**
     * Verifica si todos los procesos terminaron
     */
    private static boolean allProcessesFinished(List<Process> processes) {
        return processes.stream()
            .allMatch(p -> p.getState() == ProcessState.TERMINATED);
    }
    
    //Clona procesos para reutilizarlos
    
    private static List<Process> cloneProcesses(List<Process> original) {
        List<Process> cloned = new ArrayList<>();
        
        for (Process p : original) {
            List<Burst> clonedBursts = new ArrayList<>();
            for (Burst b : p.getBursts()) {
                clonedBursts.add(b.copy());
            }
            
            cloned.add(new Process(
                p.getPid(),
                p.getArrivalTime(),
                clonedBursts,
                p.getPriority(),
                p.getRequiredPages()
            ));
        }
        
        return cloned;
    }
    
    /**
     * Imprime tabla de procesos
     */
    private static void printProcessTable(List<Process> processes) {
        System.out.printf("%-6s %-10s %-8s %-30s %-8s\n",
            "PID", "Llegada", "Prior.", "Ráfagas", "Páginas");
        System.out.println("─".repeat(70));
        
        for (Process p : processes) {
            StringBuilder bursts = new StringBuilder();
            for (Burst b : p.getBursts()) {
                bursts.append(b.toString()).append(" ");
            }
            
            System.out.printf("%-6s %-10d %-8d %-30s %-8d\n",
                p.getPid(),
                p.getArrivalTime(),
                p.getPriority(),
                bursts.toString().trim(),
                p.getRequiredPages()
            );
        }
    }
    
    /**
     * Compara todos los algoritmos
     */
    private static void compareAlgorithms(List<Process> processes) {
        Scheduler[] schedulers = {
            new FCFS(),
            new SJF(),
            new RoundRobin(2),
            new RoundRobin(4),
            new SRT(),
            new Priority(false),
            new Priority(true)  
        };
        
        System.out.printf("\n%-22s %-12s %-12s %-12s %-12s\n",
            "Algoritmo", "Avg WT", "Avg TAT", "Avg RT", "CPU Util%");
        System.out.println("─".repeat(70));
        
        for (Scheduler scheduler : schedulers) {
            // Ejecutar silenciosamente
            List<Process> cloned = cloneProcesses(processes);
            
            // Suprimir output
            java.io.PrintStream originalOut = System.out;
            System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
            
            testAlgorithm(scheduler, cloned);
            
            // Restaurar output
            System.setOut(originalOut);
            
            // Mostrar resultados
            System.out.printf("%-22s %-12.2f %-12.2f %-12.2f %-12.2f\n",
                scheduler.getAlgorithmName(),
                scheduler.getAverageWaitingTime(),
                scheduler.getAverageTurnaroundTime(),
                scheduler.getAverageResponseTime(),
                scheduler.getCPUUtilization(),
                scheduler.getContextSwitches()
            );
        }
        
        System.out.println("\nLeyenda:");
        System.out.println("   WT  = Waiting Time (Tiempo de espera)");
        System.out.println("   TAT = Turnaround Time (Tiempo de retorno)");
        System.out.println("   RT  = Response Time (Tiempo de respuesta)");
    }

    //Clase auxiliar para manejar procesos bloqueados en E/S

    private static class IOBlock {

        Process process;
        int remainingTime;
        IOBlock(Process process, int duration) {
            this.process = process;
            this.remainingTime = duration;
        }
    }

}