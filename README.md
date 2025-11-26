<h1 align="center">Operating System Simulator – Trabajo Final</h1>

<div align="center">

> **Universidad Nacional de San Agustín**  
> **Facultad de Ingeniería de Producción y Servicios**  
> **Escuela Profesional de Ingeniería de Sistemas**  
> **Curso:** Sistemas Operativos – 2025-B

[![Made with Java](https://img.shields.io/badge/Made%20with-Java-red?style=for-the-badge&logo=java)](https://www.java.com)
[![OS Concepts](https://img.shields.io/badge/OS-Concepts-blue?style=for-the-badge&logo=linux)](https://github.com)
[![Status](https://img.shields.io/badge/Status-In%20Progress-yellow?style=for-the-badge)](https://github.com)

</div>

---

## 1. Modulos

### 1.1. Módulo de Planificación
- **FCFS** (First Come First Served)
- **SJF** (Shortest Job First)  
- **Round Robin** con quantum configurable
- Métricas: Tiempo de espera, retorno y utilización de CPU

###  1.2. Módulo de Memoria Virtual
- **FIFO** (First In First Out)
- **LRU** (Least Recently Used)
- **Algoritmo Óptimo**
- Registro de fallos de página y reemplazos

###  1.3. Módulo de Sincronización
-  Procesos como **Threads independientes**
-  Estados: `NUEVO → LISTO → EJECUTANDO → BLOQUEADO → TERMINADO`
-  Semáforos y Locks para evitar *race conditions*
-  **BONUS:** Manejo de ráfagas de E/S (+2 pts)

###  1.4. Interfaz Gráfica
-  Diagrama de Gantt interactivo
-  Tabla de páginas en tiempo real
-  Log de eventos del sistema

---
## 2. Estructura del Proyecto
```
Operating-System-Simulator/
│
├── Main.java                         # Punto de entrada
│
├── modules/
│   ├── scheduler/
│   │   ├── Scheduler.java              # Clase base abstracta
│   │   ├── FCFS.java                   # First Come First Served
│   │   ├── SJF.java                    # Shortest Job First
│   │   └── RoundRobin.java             # Round Robin
│   │
│   ├── memory/
│   │   ├── MemoryManager.java          # Gestor principal
│   │   ├── FIFO.java                   # First In First Out
│   │   ├── LRU.java                    # Least Recently Used
│   │   └── Optimal.java                # Algoritmo Óptimo
│   │
│   ├── sync/
│   │   ├── ProcessThread.java          # Thread del proceso
│   │   ├── SyncController.java         # Sincronización
│   │   └── IOManager.java              # Gestor E/S (BONUS)
│   │
│   └── gui/
│       ├── MainWindow.java             # Ventana principal
│       ├── GanttChart.java             # Diagrama de Gantt
│       └── MemoryTable.java            # Tabla de memoria
│
├── model/                            # Clases de datos
│   ├── Process.java
│   ├── ProcessState.java
│   ├── Burst.java
│   └── Config.java
│
├── utils/                           # Utilidades
│   ├── FileParser.java
│   └── Logger.java
│
├── data/                             # Archivos de prueba
│   ├── procesos.txt
│   ├── test_case_1.txt
│   └── test_case_2.txt
│
└── docs/                             # Documentación
    ├── informe_ieee.pdf
    ├── presentacion.pdf
    └── diagramas/
```

## 3. Compilacion 

### 3.1 En Ubuntu
```
mkdir -p bin
javac -d bin Main.java model/*.java utils/*.java modules/scheduler/*.java modules/memory/*.java modules/sync/*.java
java -cp bin Main data/config.txt data/procesos.txt
```


### 3.2 En Windows (powershell)

```
mkdir bin
javac -d bin Main.java model/*.java utils/*.java modules/scheduler/*.java modules/memory/*.java modules/sync/*.java
java -cp bin Main data/config.txt data/procesos.txt
```


## Requisitos GUI

