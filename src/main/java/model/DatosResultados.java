package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Clase DatosResultados
Almacena todos los resultados generados por la simulacion del sistema operativo.

OBJETIVO:
Reunir en un solo objeto todas las metricas obtenidas despues de ejecutar
los algoritmos de planificacion y administracion de memoria.

ELEMENTOS PRINCIPALES:

Metricas de graficas:
  tiempoEsperaPromedio: promedio del tiempo que los procesos permanecen en espera.
  tiempoRetornoPromedio: promedio del tiempo total que tarda cada proceso en completarse.
  tiempoRespuestaPromedio: tiempo promedio hasta la primera respuesta de CPU.
  usoCpu: porcentaje de tiempo en el que la CPU estuvo activa.
  ocioCpu: porcentaje de tiempo en el que la CPU estuvo sin uso.

Datos del planificador:
  procesosCompletados: cantidad total de procesos finalizados.
  totalProcesos: numero de procesos ingresados en el sistema.
  cambiosContexto: numero total de cambios de contexto.
  tiempoCpu: tiempo total donde la CPU estuvo ejecutando procesos.
  tiempoOcioso: tiempo total donde la CPU estuvo en inactividad.

Datos de memoria:
  cargasTotales: cantidad de cargas de paginas.
  fallosPagina: numero de fallos de pagina ocurridos.
  reemplazosPagina: numero de reemplazos realizados.
  marcosTotales: cantidad total de marcos disponibles.
  marcosLibres: cantidad de marcos sin usar durante la simulacion.

Datos generales:
  resumenProcesos:
    Lista inmutable con el resumen de cada proceso.
    Incluye tiempos de espera, retorno, respuesta y uso de CPU.
  algPlanificacion: nombre del algoritmo de planificacion utilizado.
  algMemoria: nombre del algoritmo de reemplazo de paginas utilizado.

Constructor:
  Recibe todas las metricas generadas por la simulacion
  y calcula automaticamente el porcentaje de ocio de CPU.

Uso:
Se utiliza para mostrar resultados en tablas, graficas, dashboards
y exportaciones dentro del simulador de sistema operativo.
*/


public class DatosResultados {
    //Graficas
    private final double tiempoEsperaPromedio;
    private final double tiempoRetornoPromedio;
    private final double tiempoRespuestaPromedio;
    private final double usoCpu;
    private final double ocioCpu;
    //Datos Scheduler
    private final int procesosCompletados;
    private final int totalProcesos;
    private final int cambiosContexto;
    private final int tiempoCpu;
    private final int tiempoOcioso;
    //Datos de memoria
    private final int cargasTotales;
    private final int fallosPagina;
    private final int reemplazosPagina;
    private final int marcosTotales;
    private final int marcosLibres;
    //Datos generales
    private final List<ResultadoProceso> resumenProcesos;
    private final String algPlanificacion;
    private final String algMemoria;
    public DatosResultados(
            double tiempoEsperaPromedio,
            double tiempoRetornoPromedio,
            double tiempoRespuestaPromedio,
            double usoCpu,
            int procesosCompletados,
            int totalProcesos,
            int cambiosContexto,
            int tiempoCpu,
            int tiempoOcioso,
            int cargasTotales,
            int fallosPagina,
            int reemplazosPagina,
            int marcosTotales,
            int marcosLibres,
            List<ResultadoProceso> resumenProcesos,
            String algP,
            String algM
    ) {
        this.tiempoEsperaPromedio = tiempoEsperaPromedio;
        this.tiempoRetornoPromedio = tiempoRetornoPromedio;
        this.tiempoRespuestaPromedio = tiempoRespuestaPromedio;
        this.usoCpu = usoCpu;
        this.ocioCpu = Math.max(0, 100 - usoCpu);
        this.procesosCompletados = procesosCompletados;
        this.totalProcesos = totalProcesos;
        this.cambiosContexto = cambiosContexto;
        this.tiempoCpu = tiempoCpu;
        this.tiempoOcioso = tiempoOcioso;
        this.cargasTotales = cargasTotales;
        this.fallosPagina = fallosPagina;
        this.reemplazosPagina = reemplazosPagina;
        this.marcosTotales = marcosTotales;
        this.marcosLibres = marcosLibres;
        this.resumenProcesos = new ArrayList<>(resumenProcesos);
        this.algMemoria = algM;
        this.algPlanificacion = algP;
    }

    public double getTiempoEsperaPromedio() {
        return tiempoEsperaPromedio;
    }

    public double getTiempoRetornoPromedio() {
        return tiempoRetornoPromedio;
    }

    public double getTiempoRespuestaPromedio() {
        return tiempoRespuestaPromedio;
    }

    public double getUsoCpu() {
        return usoCpu;
    }

    public double getOcioCpu() {
        return ocioCpu;
    }

    public int getProcesosCompletados() {
        return procesosCompletados;
    }

    public int getTotalProcesos() {
        return totalProcesos;
    }

    public int getCambiosContexto() {
        return cambiosContexto;
    }

    public int getFallosPagina() {
        return fallosPagina;
    }

    public int getReemplazosPagina() {
        return reemplazosPagina;
    }

    public int getCargasTotales() {
        return cargasTotales;
    }

    public int getMarcosTotales() {
        return marcosTotales;
    }

    public int getMarcosLibres() {
        return marcosLibres;
    }

    public int getTiempoCpu() {
        return tiempoCpu;
    }

    public int getTiempoOcioso() {
        return tiempoOcioso;
    }

    public List<ResultadoProceso> getResumenProcesos() {
        return Collections.unmodifiableList(resumenProcesos);
    }

    public String getAlgPlanificacion() {
        return algPlanificacion;
    }

    public String getAlgMemoria() {
        return algMemoria;
    }

    public static DatosResultados prueba() {
        List<ResultadoProceso> procesos = List.of(
                new ResultadoProceso("P1", 8, 18, 5, 150),
                new ResultadoProceso("P2", 15, 25, 8, 310),
                new ResultadoProceso("P3", 22, 40, 11, 220),
                new ResultadoProceso("P4", 10, 15, 6, 452),
                new ResultadoProceso("P5", 25, 45, 15, 320)
        );

        return new DatosResultados(
                12.5,
                28.1,
                10.8,
                87.0,
                5,
                5,
                47,
                1250,
                182,
                2105,
                1452,
                312,
                64,
                12,
                procesos,
                "Algoritmo Round Robin",
                "Algoritmo FIFO"
        );
    }
}


