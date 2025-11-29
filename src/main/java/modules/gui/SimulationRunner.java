package modules.gui;

import java.util.List;

import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import modules.sync.SimulationStateListener;
import modules.gui.dashboard.MemPanel;
import modules.gui.dashboard.ProPanel;
import utils.FileParser;
import utils.Logger;
import utils.SimulationFactory;

public class SimulationRunner {

    public static void runSimulation(Config config, String processPath, ProPanel proPanel, MemPanel memPanel) throws Exception {
        
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
            System.out.println("[SimulationRunner] Registrando MemoryVisualizer en MemoryManager...");
            memoryManager.addListener(memPanel.getVisualizer());
        } else {
            System.out.println("[SimulationRunner] MemPanel o visualizer es NULL. No se registro listener de memoria.");
        }


        printSystemConfiguration(config, processes, scheduler, memoryManager);
        System.out.println();
        SimulationEngine engine = new SimulationEngine(
            scheduler, memoryManager, processes, config
        );
        
        //REGISTRAR EL LISTENER ANTES DE INICIAR
        if (proPanel != null) {
            System.out.println("[SimulationRunner] Registrando listener en el engine...");
            
            engine.setStateListener(new SimulationStateListener() {
                @Override
                public void onReadyQueueChanged(List<Process> readyQueue) {
                    System.out.println("[SimulationRunner]  Ready queue actualizada: " + readyQueue.size());
                    proPanel.updateReadyQueue(readyQueue);
                }

                @Override
                public void onBlockedIOChanged(List<Process> blockedIO) {
                    System.out.println("[SimulationRunner]  Blocked I/O actualizada: " + blockedIO.size());
                    proPanel.updateBlockedIO(blockedIO);
                }

                @Override
                public void onBlockedMemoryChanged(List<Process> blockedMemory) {
                    System.out.println("[SimulationRunner]  Blocked Memory actualizada: " + blockedMemory.size());
                    proPanel.updateBlockedMemory(blockedMemory);
                }

                @Override
                public void onProcessStateChanged(Process process) {
                    // Implementar si es necesario
                }

                @Override
                public void onTimeChanged(int currentTime) {
                    System.out.println("[SimulationRunner]  Tiempo: " + currentTime);
                }
            });
        } else {
            System.out.println("[SimulationRunner]  ProPanel es NULL, no se registro listener");
        }
        
        // EJECUTAR EN THREAD SEPARADO PARA NO BLOQUEAR LA UI
        Thread simulationThread = new Thread(() -> {
            try {
                System.out.println("[SimulationRunner]  Iniciando simulacion en thread separado...");
                engine.run();
                System.out.println("[SimulationRunner]  Simulacion completada");
            } catch (Exception e) {
                System.err.println("[SimulationRunner]  Error en simulacion: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SimulationThread");
        
        simulationThread.setDaemon(false); // No es daemon para que complete antes de cerrar
        simulationThread.start();
        
        System.out.println("[SimulationRunner] Thread de simulacion iniciado");
    }
    
    private static void printSystemConfiguration(Config config, List<Process> processes, Scheduler scheduler, MemoryManager memoryManager) {
        
        System.out.println();
        Logger.log("CONFIGURACION DEL SISTEMA:");
        Logger.log("Recuerda:  Cada proceso tiene sus propias paginas y no se mezclan, solo se comparte la memoria fisica(Frames)");
    //Una pagina solo necesita un frame libre donde colocarse. La tabla de paginas es la que guarda esa relacion.
        Logger.log("    Algoritmo de planificacion: " + scheduler.getAlgorithmName());
        Logger.log("    Algoritmo de reemplazo: " + memoryManager.getAlgorithmName());
        Logger.log("    Marcos de memoria: " + config.getTotalFrames());
        Logger.log("    Tamaño de marco: " + config.getFrameSize() + " bytes");
        Logger.log("    Quantum (RR): " + config.getQuantum() + " unidades");
        Logger.log("    Unidad de tiempo: " + config.getTimeUnit() + " ms");
        Logger.log("    Numero de procesos: " + processes.size() + "\n");
        
        Logger.log("PROCESOS CARGADOS:");
        for (Process process : processes) {
            Logger.log(String.format(
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