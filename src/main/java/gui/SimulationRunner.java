package gui;

import model.Config;
import model.Process;
import modules.memory.MemoryManager;
import modules.scheduler.FCFS;
import modules.scheduler.Scheduler;
import modules.sync.SimulationEngine;
import utils.FileParser;
import utils.Logger;

import java.util.List;

public class SimulationRunner {

    public static void runSimulation(String configPath, String processPath) throws Exception {

        Logger.section("CARGANDO CONFIGURACIÓN");
        Config config = FileParser.parseConfig(configPath);

        if (!config.validate()) {
            throw new Exception("Configuración inválida.");
        }

        Logger.section("CARGANDO PROCESOS");
        List<Process> processes = FileParser.parseProcesses(processPath);

        if (processes.isEmpty()) {
            throw new Exception("No se encontraron procesos.");
        }

        Scheduler scheduler = new FCFS(); // usa tu método si quieres
        MemoryManager memoryManager = new modules.memory.FIFO(config.getTotalFrames());

        SimulationEngine engine = new SimulationEngine(
                scheduler, memoryManager, processes, config
        );

        engine.run();
        Logger.printSummary();
    }
}
