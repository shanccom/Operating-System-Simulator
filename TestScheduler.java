import model.Burst;
import model.Burst.BurstType;
import model.Process;
import model.ProcessState;
import modules.scheduler.FCFS;
import modules.scheduler.SJF;
import modules.scheduler.RoundRobin;
import modules.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**Clase de prueba INDEPENDIENTE para el módulo de planificación 
 * No requiere memoria, GUI, ni otros módulos
 *  javac TestScheduler.java model/*.java modules/scheduler/*.java
 * java TestScheduler
 */
public class TestScheduler {
    
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
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
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
        
        // Comparación final
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ COMPARACIÓN DE ALGORITMOS                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        compareAlgorithms(testProcesses);
    }
    
    /**
     * Crea procesos de prueba
     * P1: Llega en t=0, CPU(4) 
     * P2: Llega en t=2, CPU(6)
     * P3: Llega en t=4, CPU(3)
     */
    private static List<Process> createTestProcesses() {
        List<Process> processes = new ArrayList<>();
        
        // Proceso P1
        processes.add(new Process(
            "P1",
            0,
            Arrays.asList(
                new Burst(BurstType.CPU, 4)
            ),
            1,
            4
        ));
        
        // Proceso P2
        processes.add(new Process(
            "P2",
            2,
            Arrays.asList(
                new Burst(BurstType.CPU, 6)
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

        Process currentProcess = null;
        Process preemptedProcess = null;   // ← proceso expulsado por quantum

        while (currentTime < maxTime) {

            // 1. Verificar nuevas llegadas
            for (Process p : processes) {
                if (p.getArrivalTime() == currentTime && p.getState() == ProcessState.NEW) {
                    p.setState(ProcessState.READY);
                    scheduler.addProcess(p);
                    System.out.println("[t=" + currentTime + "] " + p.getPid() + " → READY");
                }
            }

            //Aquí se agrega el preemptedProcess (si viene de la iteración anterior)
            if (preemptedProcess != null) {
                scheduler.addProcess(preemptedProcess);
                preemptedProcess = null;
            }

            // 2. Actualizar tiempos de espera
            for (Process p : scheduler.getReadyQueueSnapshot()) {
                p.updateWaitingTime(currentTime);
            }

            // 3. Seleccionar proceso si CPU está libre
            if (currentProcess == null || currentProcess.getState() != ProcessState.RUNNING) {
                
                currentProcess = scheduler.selectNextProcess();
                if (currentProcess != null) {
                    currentProcess.setState(ProcessState.RUNNING);
                    currentProcess.markFirstExecution(currentTime);
                }
            }

            // Si no hay proceso actual, avanzar tiempo
            if (currentProcess == null) {
                currentTime++;
                scheduler.setCurrentTime(currentTime);
                continue;
            }

            // 4. Ejecutar ráfaga de CPU
            Burst burst = currentProcess.getCurrentBurst();

            if (burst != null && burst.isCPU()) {

                boolean burstCompleted = burst.execute(1);
                scheduler.recordCPUTime(1);

                System.out.printf("[t=%d] %s ejecutando %s (restante: %d)\n",
                    currentTime, currentProcess.getPid(), burst.getType(),
                    burst.getRemainingTime());

                // 5. Verificar si terminó la ráfaga
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
                        // Suponemos que NO hay IO real (tu test simplificado)
                        // devolverlo a NEW para reencolar en la siguiente iteración
                        currentProcess.setState(ProcessState.NEW);
                        preemptedProcess = currentProcess;
                        currentProcess = null;
                        // NOTA: NO avanzamos tiempo aquí; el bloque al final lo hará.
                    }

                } else {

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

                            // IMPORTANTÍSIMO: avanzar el tiempo aquí para que la reinserción
                            // ocurra en el siguiente tick (y así las llegadas del siguiente tick
                            // sean procesadas primero).
                            currentTime++;
                            scheduler.setCurrentTime(currentTime);

                            // Saltamos al inicio del bucle (en el nuevo tiempo)
                            continue;
                        }
                    }

                }
            }

            // --- Eliminé la inserción duplicada que tenías AL FINAL ---
            // (ya se inserta arriba, justo después de procesar llegadas)

            currentTime++;
            scheduler.setCurrentTime(currentTime);
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
    
    /**
     * Clona procesos para reutilizarlos
     */
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
            new RoundRobin(4)
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
                scheduler.getCPUUtilization()
            );
        }
        
        System.out.println("\nLeyenda:");
        System.out.println("   WT  = Waiting Time (Tiempo de espera)");
        System.out.println("   TAT = Turnaround Time (Tiempo de retorno)");
        System.out.println("   RT  = Response Time (Tiempo de respuesta)");
    }
}