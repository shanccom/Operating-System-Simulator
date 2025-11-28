package model;

public class ResultadoProceso {
    private final String pid;
    private final int tiempoEspera;
    private final int tiempoRetorno;
    private final int tiempoRespuesta;
    private final int fallosPagina;
    private final int reemplazos;

    public ResultadoProceso(String pid, int tiempoEspera, int tiempoRetorno, int tiempoRespuesta, int fallosPagina, int reemplazos) {
        this.pid = pid;
        this.tiempoEspera = tiempoEspera;
        this.tiempoRetorno = tiempoRetorno;
        this.tiempoRespuesta = tiempoRespuesta;
        this.fallosPagina = fallosPagina;
        this.reemplazos = reemplazos;
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

    public int getReemplazos() {
        return reemplazos;
    }
}
