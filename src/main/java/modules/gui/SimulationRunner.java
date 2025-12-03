package modules.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Config;
import model.DatosResultados;
import model.Process;
import modules.gui.pages.DashboardPage; 
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import modules.sync.SimulationStateListener;

import modules.gui.dashboard.MemPanel;

import modules.gui.pages.ResultadosPage;

import utils.FileParser;
import utils.Logger;
import utils.SimulationFactory;


public class SimulationRunner {


    public static void runSimulation(Config config, String processPath, DashboardPage dashboardPage, MemPanel memPanel, MainFX mainFx) throws Exception {

        
        if (!config.validate()) {
            throw new IllegalArgumentException("Configuracion invalida");
        }
    
        List<Process> processes = FileParser.parseProcesses(processPath);
        if (processes.isEmpty()) {
            throw new Exception("No se encontraron procesos para simular");
        }
        Scheduler scheduler = SimulationFactory.createScheduler(config);
        MemoryManager memoryManager = SimulationFactory.createMemoryManager(config);
        // ---- registrar visualizador de memoria (si existe)
        if (memPanel != null && memPanel.getVisualizer() != null) {
            memoryManager.addListener(memPanel.getVisualizer());
        } else {
        }


        printSystemConfiguration(config, processes, scheduler, memoryManager);
        
        System.out.println();
        SimulationEngine engine = new SimulationEngine(
            scheduler, memoryManager, processes, config
        );
        
        if (mainFx != null && mainFx.getConfigPage() != null) {
          javafx.application.Platform.runLater(() -> {
              mainFx.getConfigPage().setCurrentEngine(engine);
              System.out.println("[SimulationRunner] Engine guardado en ConfigPage");
          });
          
          try {
              Thread.sleep(300);
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
        }

        //REGISTRAR EL LISTENER ANTES DE INICIAR

        if (dashboardPage  != null) {
            //System.out.println("[SimulationRunner] Registrando listener en el engine...");

            //limpiamos el diagrama
            dashboardPage.getExePanel().clearGantt();
            List<String> processIds = processes.stream()
                .map(Process::getPid)
                .toList();
            dashboardPage.getExePanel().initializeProcesses(processIds);
            
            engine.setStateListener(new SimulationStateListener() {

                //para el diagrama de Gantt
                private Map<String, Integer> executionStarts = new HashMap<>();

                @Override
                public void onProcessExecutionStarted(String pid, int startTime) {
                    //System.out.println("[SimulationRunner]INICIO de ejecución → PID=" + pid + ", t=" + startTime);
                    dashboardPage.getExePanel().addExecutionStart(pid, startTime);
                    executionStarts.put(pid, startTime);
                    dashboardPage.getExePanel().setCurrentTime(startTime);
                }

                @Override
                public void onProcessExecutionEnded(String pid, int endTime) {
                    Integer start = executionStarts.get(pid);
                    //System.out.println("[SimulationRunner]FIN de ejecución → PID=" + pid +", inicio=" + start + ", fin=" + endTime);
                    dashboardPage.getExePanel().addExecutionEnd(pid, endTime);
                    executionStarts.remove(pid);
                }
                @Override
                public void onIOStarted(String pid, int startTime){
                    //System.out.println("[SimulationRunner] I/O iniciado → PID=" + pid + ", t=" + startTime);
                    dashboardPage.getExePanel().addIOStart(pid, startTime);
                }
                @Override
                public void onIOEnded(String pid, int endTime){
                    //System.out.println("[SimulationRunner] I/O terminado → PID=" + pid + ", t=" + endTime);
                    dashboardPage.getExePanel().addIOEnd(pid, endTime);
                }

                @Override
                public void onContextSwitch(String pid, int startTime, int duration) {
                    System.out.println("[SimulationRunner] Context Switch detectado: " + pid + 
                                    " en t=" + startTime + ", duración=" + duration);
                    
                    // Incrementar contador de context switches
                    dashboardPage.getExePanel().incrementContextSwitch();
                    
                    // Agregar el bloque visual de context switch en el diagrama de Gantt
                    dashboardPage.getExePanel().addContextSwitchBlock(pid, startTime, duration);
                }
                

                //para las colas de procesos
                @Override
                public void onReadyQueueChanged(List<Process> readyQueue) {

                    //System.out.println("[SimulationRunner]  Ready queue actualizada: " + readyQueue.size());
                    dashboardPage.getProPanel().updateReadyQueue(readyQueue);

                }

                @Override
                public void onBlockedIOChanged(List<Process> blockedIO) {

                    //System.out.println("[SimulationRunner]  Blocked I/O actualizada: " + blockedIO.size());
                    dashboardPage.getProPanel().updateBlockedIO(blockedIO);

                }

                @Override
                public void onBlockedMemoryChanged(List<Process> blockedMemory) {

                    //System.out.println("[SimulationRunner]  Blocked Memory actualizada: " + blockedMemory.size());
                    dashboardPage.getProPanel().updateBlockedMemory(blockedMemory);

                }

                @Override
                public void onProcessStateChanged(Process process) {
                    // aun nada
                }

                @Override
                public void onTimeChanged(int currentTime) {
                    //System.out.println("[SimulationRunner]  Tiempo cambiado: " + currentTime);
                    dashboardPage.getExePanel().setCurrentTime(currentTime);
                }
            });
        } else {
        }
        
        // EJECUTAR EN THREAD SEPARADO PARA NO BLOQUEAR LA UI
        Thread simulationThread = new Thread(() -> {
            try {
                engine.run();
                DatosResultados resultados = engine.getDatosFinales();

                javafx.application.Platform.runLater(() -> {
                    ResultadosPage resultadosPage = new ResultadosPage(resultados);
                    mainFx.showResultados(resultadosPage);  
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SimulationThread");

        simulationThread.setDaemon(false); // No es daemon para que complete antes de cerrar
        simulationThread.start();
        
    }
    
    private static void printSystemConfiguration(Config config, List<Process> processes, Scheduler scheduler, MemoryManager memoryManager) {
        
        System.out.println(); //ESte si imprime 
        Logger.syncLog("CONFIGURACION DEL SISTEMA:");
        Logger.syncLog("Recuerda:  Cada proceso tiene sus propias paginas y no se mezclan, solo se comparte la memoria fisica(Frames)");
    //Una pagina solo necesita un frame libre donde colocarse. La tabla de paginas es la que guarda esa relacion.
        Logger.syncLog("    Algoritmo de planificacion: " + scheduler.getAlgorithmName());
        Logger.syncLog("    Algoritmo de reemplazo: " + memoryManager.getAlgorithmName());
        Logger.syncLog("    Marcos de memoria: " + config.getTotalFrames());
        Logger.syncLog("    Tamaño de marco: " + config.getFrameSize() + " bytes");
        Logger.syncLog("    Quantum (RR): " + config.getQuantum() + " unidades");
        Logger.syncLog("    Unidad de tiempo: " + config.getTimeUnit() + " ms");
        Logger.syncLog("    Numero de procesos: " + processes.size() + "\n");
        
        Logger.syncLog("PROCESOS CARGADOS:");
        for (Process process : processes) {
            Logger.syncLog(String.format(
                "   %s: Llegada = %d, Prioridad = %d, Rafagas = %d, Memoria = %d paginas",
                process.getPid(),
                process.getArrivalTime(),
                process.getPriority(),
                process.getBursts().size(),
                process.getRequiredPages()
            ));
        }
    }
}