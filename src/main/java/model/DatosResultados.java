package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatosResultados {
    private final double tiempoEsperaPromedio;
    private final double tiempoRetornoPromedio;
    private final double tiempoRespuestaPromedio;
    private final double usoCpu;
    private final double ocioCpu;
    private final int fallosPagina;
    private final int reemplazosPagina;
    private final int marcosTotales;
    private final int marcosLibres;
    private final int tiempoCpu;
    private final int tiempoOcioso;
    private final List<ResultadoProceso> resumenProcesos;
    private final String algPlanificacion;
    private final String algMemoria;
    private final int totalProcesos;
    public DatosResultados(
            double tiempoEsperaPromedio,
            double tiempoRetornoPromedio,
            double tiempoRespuestaPromedio,
            double usoCpu,
            int fallosPagina,
            int reemplazosPagina,
            int tiempoCpu,
            int tiempoOcioso,
            int marcosTotales,
            int marcosLibres,
            List<ResultadoProceso> resumenProcesos,
            String algP,
            String algM,
            int totalP
    ) {
        this.tiempoEsperaPromedio = tiempoEsperaPromedio;
        this.tiempoRetornoPromedio = tiempoRetornoPromedio;
        this.tiempoRespuestaPromedio = tiempoRespuestaPromedio;
        this.usoCpu = usoCpu;
        this.ocioCpu = Math.max(0, 100 - usoCpu);
        this.fallosPagina = fallosPagina;
        this.reemplazosPagina = reemplazosPagina;
        this.tiempoCpu = tiempoCpu;
        this.tiempoOcioso = tiempoOcioso;
        this.marcosTotales = marcosTotales;
        this.marcosLibres = marcosLibres;
        this.resumenProcesos = new ArrayList<>(resumenProcesos);
        this.algMemoria = algM;
        this.algPlanificacion = algP;
        this.totalProcesos = totalP;
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

    public int getFallosPagina() {
        return fallosPagina;
    }

    public int getReemplazosPagina() {
        return reemplazosPagina;
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

}
