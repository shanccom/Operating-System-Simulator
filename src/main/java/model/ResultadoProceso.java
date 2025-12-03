package model;

/*
Clase ResultadoProceso
Guarda el resumen final de un proceso después de la simulación.

OBJETIVO:
Almacenar los datos principales que se muestran en tablas o reportes,
como tiempos y fallos de página.

DATOS QUE GUARDA:
  pid: identificador del proceso.
  tiempoEspera: cuánto tiempo estuvo en READY.
  tiempoRetorno: tiempo total desde que llegó hasta que terminó.
  tiempoRespuesta: cuánto tardó en ejecutarse por primera vez.
  fallosPagina: cantidad de fallos de página del proceso.

USO:
Se crea al final de la simulación para mostrar los resultados
de cada proceso sin modificar el objeto original.
*/


public class ResultadoProceso {
    private final String pid;
    private final int tiempoEspera;
    private final int tiempoRetorno;
    private final int tiempoRespuesta;
    private final int fallosPagina;

    public ResultadoProceso(String pid, int tiempoEspera, int tiempoRetorno, int tiempoRespuesta, int fallosPagina) {
        this.pid = pid;
        this.tiempoEspera = tiempoEspera;
        this.tiempoRetorno = tiempoRetorno;
        this.tiempoRespuesta = tiempoRespuesta;
        this.fallosPagina = fallosPagina;
    }

    public String getPid() {
        return pid;
    }

    public int getTiempoEspera() {
        return tiempoEspera;
    }

    public int getTiempoRetorno() {
        return tiempoRetorno;
    }

    public int getTiempoRespuesta() {
        return tiempoRespuesta;
    }

    public int getFallosPagina() {
        return fallosPagina;
    }

}
