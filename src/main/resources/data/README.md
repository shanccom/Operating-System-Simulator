# Pruebas
## Configuración recomendada

Caso 1: 12 frames, quantum=4 (en RR)
Caso 2: 10 frames,  quantum=3 (en RR)
Caso 3: 6 frames,  quantum=3 (thrashing severo) (en RR)
Caso 4: 8 frames,  quantum=4 (acceso secuencial) (en RR)
Caso 5: 8 frames,  quantum=3 (alta localidad) (en RR)

## Resultados

Caso 1: Todos funcionan bien (frames suficientes)
Caso 2: LRU > FIFO, Optimal mejo r
Caso 3: Muchos page faults en todos, Optimal reduce más
Caso 4: FIFO ≈ LRU (acceso secuencial)
Caso 5: LRU >> FIFO (localidad temporal)