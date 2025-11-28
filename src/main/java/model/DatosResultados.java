package model;

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
            int marcosLibres
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
    }

}
