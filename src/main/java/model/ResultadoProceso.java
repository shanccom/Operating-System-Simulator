package model;

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
