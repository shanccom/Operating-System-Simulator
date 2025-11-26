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
├── pom.xml                        # Configuración Maven + JavaFX
│
├── src/
│   └── main/
│        ├── java/
│        │   ├── Main.java              
│        │   │
│        │   ├── modules/
│        │   │   ├── scheduler/
│        │   │   │   ├── Scheduler.java
│        │   │   │   ├── FCFS.java
│        │   │   │   ├── SJF.java
│        │   │   │   └── RoundRobin.java
│        │   │   │
│        │   │   ├── memory/
│        │   │   │   ├── MemoryManager.java
│        │   │   │   ├── FIFO.java
│        │   │   │   ├── LRU.java
│        │   │   │   └── Optimal.java
│        │   │   │
│        │   │   ├── sync/
│        │   │   │   ├── ProcessThread.java
│        │   │   │   ├── SyncController.java
│        │   │   │   └── IOManager.java
│        │   │   │
│        │   │   └── gui/
│        │   │       ├── MainWindow.java
│        │   │       ├── GanttChart.java
│        │   │       └── MemoryTable.java
│        │   │
│        │   ├── model/
│        │   │   ├── Process.java
│        │   │   ├── ProcessState.java
│        │   │   ├── Burst.java
│        │   │   └── Config.java
│        │   │
│        │   └── utils/
│        │       ├── FileParser.java
│        │       └── Logger.java
│        │
│        └── resources/
│            ├── data/                 
│                ├── procesos.txt
│                ├── test_case_1.txt
│                └── test_case_2.txt
│                         
│
└── docs/
    ├── informe_ieee.pdf
    ├── presentacion.pdf
    └── diagramas/

```

## 3. Compilacion 

### 3.1 En Ubuntu
Requiere Maven y JDK 17.
```
mvn javafx:run
```


### 3.2 En Windows (powershell)

Requiere Maven y JDK 17.
- Ir a la página oficial: Apache Maven Download https://maven.apache.org/download.cgi 	
- Descargar el archivo apache-maven-3.9.11-bin.zip (Binary zip archive).
C:\Program Files\Apache\Maven\apache-maven-3.9.x
Agregarlo al path


## Desplegar ...
```
mvn clean install
mvn javafx:run
```
