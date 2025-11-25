import model.Burst;
import model.Burst.BurstType;
import model.Process;
import model.ProcessState;
import modules.scheduler.FCFS;
import modules.scheduler.SJF;
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
        
        // Probar cada algoritmo
        System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ PRUEBA 1: FCFS (First Come First Served)            ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        testAlgorithm(new FCFS(), cloneProcesses(testProcesses));
        
        /*System.out.println("\n\n╔═══════════════════════════════════════════════════════╗");
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
     * P3: Llega en t=4, CPU(8)
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
                new Burst(BurstType.CPU, 8)
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
                scheduler.addProcess(p);
                System.out.println("[t=" + currentTime + "] " + p.getPid() + " → READY");
            }
        }
        
        // Simulación

        Process currentProcess = null;
        while (currentTime < maxTime) {
            // 1. Verificar nuevas llegadas
            for (Process p : processes) {
                if (p.getArrivalTime() == currentTime && p.getState() == ProcessState.NEW) {
                    p.setState(ProcessState.READY);
                    scheduler.addProcess(p);
                    System.out.println("[t=" + currentTime + "] " + p.getPid() + " → READY");
                }
            }
            
            // 2. Actualizar tiempos de espera
            for (Process p : scheduler.getReadyQueueSnapshot()) {
                p.updateWaitingTime(currentTime);
            }
            
            // 3. Seleccionar proceso
            if (currentProcess == null || currentProcess.getState() != ProcessState.RUNNING) {
                System.out.println("[DEBUG t=" + currentTime + "] Cola READY tiene " + 
                                 scheduler.getReadyQueueSize() + " procesos");
                
                currentProcess = scheduler.selectNextProcess();
                
                if (currentProcess == null) {
                    // No hay procesos listos
                    if (allProcessesFinished(processes)) {
                        System.out.println("[t=" + currentTime + "] ✓ Todos los procesos terminados");
                        break;
                    }
                    scheduler.recordIdleTime(1);
                    System.out.println("[t=" + currentTime + "] CPU IDLE");
                    currentTime++;
                    continue;
                }
                
                System.out.println("[t=" + currentTime + "] ✓ Proceso seleccionado: " + currentProcess.getPid());
                currentProcess.setState(ProcessState.RUNNING);
                currentProcess.markFirstExecution(currentTime);
            }
            // 4. Ejecutar proceso actual
            Burst burst = currentProcess.getCurrentBurst();
            if (burst != null) {
                // Solo ejecutar ráfagas de CPU
                if (burst.isCPU()) {
                    boolean burstCompleted = burst.execute(1);
                    scheduler.recordCPUTime(1);
                    
                    System.out.printf("[t=%d] %s ejecutando %s (restante: %d)\n",
                        currentTime, currentProcess.getPid(), burst.getType(), 
                        burst.getRemainingTime());
                    
                    // 5. Verificar si terminó la ráfaga
                    if (burstCompleted) {
                        System.out.println("[t=" + currentTime + "] " + currentProcess.getPid() + 
                                         " completó ráfaga " + burst.getType());
                        
                        currentProcess.advanceBurst();
                        
                        if (currentProcess.isCompleted()) {
                            // Proceso terminado
                            currentProcess.setState(ProcessState.TERMINATED);
                            currentProcess.setCompletionTime(currentTime + 1);
                            scheduler.onProcessComplete(currentProcess);
                            System.out.println("[t=" + currentTime + "] " + currentProcess.getPid() + 
                                             " → TERMINATED");
                            currentProcess = null; // Liberar CPU
                        } else {
                            // Siguiente ráfaga
                            Burst nextBurst = currentProcess.getCurrentBurst();
                            if (nextBurst != null && nextBurst.isIO()) {
                                // Simular E/S bloqueante
                                currentProcess.setState(ProcessState.BLOCKED_IO);
                                System.out.println("[t=" + currentTime + "] " + currentProcess.getPid() + 
                                                 " → BLOCKED_IO");
                                // TODO: En implementación completa, manejar E/S en cola separada
                                // Por ahora, simulamos E/S instantánea y lo devolvemos
                                currentProcess.setState(ProcessState.NEW);
                                scheduler.addProcess(currentProcess);
                                currentProcess = null; // Liberar CPU
                            } else {
                                // Otra ráfaga de CPU, devolver a ready
                                currentProcess.setState(ProcessState.NEW);
                                scheduler.addProcess(currentProcess);
                                currentProcess = null; // Liberar CPU
                            }
                        }
                    } else {
                        // Ráfaga no completada - verificar si debe ser interrumpida
                        
                        /*  Para Round Robin: verificar quantum
                        if (scheduler instanceof RoundRobin) {
                            RoundRobin rr = (RoundRobin) scheduler;
                            rr.decrementQuantum();
                            
                            if (rr.isQuantumExpired()) {
                                System.out.println("[t=" + currentTime + "] " + 
                                                 currentProcess.getPid() + " → PREEMPTED (quantum)");
                                currentProcess.setState(ProcessState.NEW);
                                scheduler.addProcess(currentProcess);
                                currentProcess = null; // Liberar CPU para cambio de contexto
                            }
                            // Si no expiró, continuar ejecutando el mismo proceso
                        }
                            */
                        // Para FCFS y SJF: continuar ejecutando sin interrupción
                    }
                } else {
                    // Ráfaga de E/S (no debería llegar aquí en ejecución normal)
                    System.out.println("[ERROR] Ráfaga de E/S en ejecución de CPU");
                    currentProcess = null;
                }
            }
            
            currentTime++;
            scheduler.setCurrentTime(currentTime);
        }
        
        if (currentTime >= maxTime) {
            System.out.println("\n⚠️  Simulación detenida por límite de tiempo");
        }
        
        // Mostrar métricas
        System.out.println("\n" + "─".repeat(60));
        scheduler.printMetrics();
        
        // Detalles por proceso
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
            new SJF()
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
        
        System.out.println("\n💡 Leyenda:");
        System.out.println("   WT  = Waiting Time (Tiempo de espera)");
        System.out.println("   TAT = Turnaround Time (Tiempo de retorno)");
        System.out.println("   RT  = Response Time (Tiempo de respuesta)");
    }
}