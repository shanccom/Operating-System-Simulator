package modules.gui;

import java.util.List;

import model.Config;
import model.DatosResultados;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import modules.sync.SimulationStateListener;
import modules.gui.dashboard.MemPanel;
import modules.gui.dashboard.ProPanel;
import modules.gui.pages.ResultadosPage;
import utils.FileParser;
import utils.Logger;
import utils.SimulationFactory;

public class SimulationRunner {

    public static void runSimulation(Config config, String processPath, ProPanel proPanel, MemPanel memPanel, MainFX mainFx) throws Exception {
        
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
        
        //REGISTRAR EL LISTENER ANTES DE INICIAR
        if (proPanel != null) {
            
            engine.setStateListener(new SimulationStateListener() {
                @Override
                public void onReadyQueueChanged(List<Process> readyQueue) {
                    proPanel.updateReadyQueue(readyQueue);
                }

                @Override
                public void onBlockedIOChanged(List<Process> blockedIO) {
                    proPanel.updateBlockedIO(blockedIO);
                }

                @Override
                public void onBlockedMemoryChanged(List<Process> blockedMemory) {
                    proPanel.updateBlockedMemory(blockedMemory);
                }

                @Override
                public void onProcessStateChanged(Process process) {
                    // Implementar si es necesario
                }

                @Override
                public void onTimeChanged(int currentTime) {
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