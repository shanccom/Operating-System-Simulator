# Simulador Integrado de Planificación de Procesos y Gestión de Memoria Virtual  

## Descripción General
Este proyecto implementa un simulador educativo de un sistema operativo simplificado que integra:

- Planificación de procesos (CPU Scheduling).
- Gestión de memoria virtual (Page Replacement).
- Simulación concurrente mediante hilos.
- Mecanismos de sincronización para evitar condiciones de carrera.

El simulador permite observar cómo las políticas de planificación y de reemplazo de páginas afectan el rendimiento global del sistema.

## Componentes del Simulador

### 1. Módulo de Planificación de CPU
- Mantiene la cola de procesos listos.
- Aplica el algoritmo seleccionado.
- Coordina con el módulo de memoria antes de ejecutar cada ráfaga.
- Gestiona el cambio de contexto.

### 2. Módulo de Gestión de Memoria Virtual
- Simula la memoria principal dividida en marcos.
- Cada proceso requiere un número de páginas.
- Aplica el algoritmo de reemplazo elegido.
- Registra fallos de página, reemplazos y estado de los marcos.

### 3. Módulo de Sincronización
- Controla el acceso concurrente del planificador, los hilos y la memoria.
- Evita condiciones de carrera en colas y tablas.
- Gestiona bloqueos y desbloqueos por memoria o E/S.

### 4. Procesos Simulados
- Cada proceso se modela como un hilo.
- Estados: Nuevo, Listo, Ejecutando, Bloqueado, Terminado.
- Pueden tener ráfagas alternadas CPU/E/S.

---

## Entradas del Simulador
El sistema recibe:

- PID  
- Tiempo de llegada  
- Ráfagas de CPU y E/S  
- Prioridad (opcional)  
- Páginas requeridas  

### Ejemplo (procesos.txt):
```
P1 0 CPU(4),E/S(3),CPU(5) 1 4
P2 2 CPU(6),E/S(2),CPU(3) 2 5
P3 4 CPU(8) 3 6
```

### Parámetros adicionales:
- Número de marcos totales.
- Algoritmo de planificación.
- Algoritmo de reemplazo de páginas.
- Quantum (si aplica).
