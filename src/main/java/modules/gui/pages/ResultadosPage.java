package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;   // <-- IMPORTANTE
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.DatosResultados;
import model.ResultadoProceso;

public class ResultadosPage extends VBox {

    // Contenedor interno que irá dentro del ScrollPane
    private final VBox content = new VBox(20);

    private final Label algPlanLabel = new Label();
    private final Label algMemLabel = new Label();
    private final Label totalProcesosLabel = new Label();
    private final Label procesosCompletadosLabel = crearValorPrincipal();
    private final Label tiempoRespuestaLabel = crearValorPrincipal();
    private final Label cambiosContextoLabel = crearValorPrincipal();
    private final Label tiempoCpuLabel = crearValorPrincipal();
    private final Label tiempoOciosoLabel = crearValorPrincipal();
    private final Label cargasTotalesLabel = crearValorPrincipal();
    private final Label fallosPaginaLabel = crearValorPrincipal();
    private final Label reemplazosLabel = crearValorPrincipal();
    private final Label marcosLibresLabel = crearValorPrincipal();
    private final ProgressIndicator graficaCpu = new ProgressIndicator();
    private final Label porcentajeCpuLabel = new Label();
    private final Label estadoCpu = new Label();
    private final VBox contenedorBarras = new VBox(10);
    private final TableView<ResultadoProceso> tablaProcesos = new TableView<>();

    public ResultadosPage() {
        this(DatosResultados.prueba());
    }

    public ResultadosPage(DatosResultados datos) {
        // El VBox raíz ahora solo sostiene el ScrollPane
        getStyleClass().add("page-container");
        setPadding(new Insets(0));
        setSpacing(0);

        // ScrollPane similar al de ConfigPage
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        content.setSpacing(20);
        content.setPadding(new Insets(20));

        scrollPane.setContent(content);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);

        // A partir de aquí, TODO se agrega a "content" en vez de al VBox raíz
        construirEncabezado();

        content.getChildren().addAll(
                construirDatosGenerales(),
                construirBloqueMetricasJuntas()
        );
        construirVisualizaciones();
        construirTabla();
        actualizarDatos(datos);
    }

    private void construirEncabezado() {
        BorderPane barra = new BorderPane();
        barra.setPadding(new Insets(0, 0, 3, 0));

        VBox textos = new VBox(4);
        Label titulo = new Label("Resultados de Simulación");
        titulo.getStyleClass().add("page-title");
        textos.getChildren().addAll(titulo);

        barra.setLeft(textos);
        VBox.setMargin(barra, new Insets(0, 0, 3, 0));
        content.getChildren().add(barra);   // <-- antes era getChildren()
    }

    private Node construirDatosGenerales() {
        VBox contenedor = new VBox(10);
        Label titulo = new Label("Datos Generales");
        titulo.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);

        grid.add(crearDatoResumido("Algoritmo de Planificación", algPlanLabel), 0, 0);
        grid.add(crearDatoResumido("Algoritmo de Memoria", algMemLabel), 1, 0);
        grid.add(crearDatoResumido("Total de Procesos", totalProcesosLabel), 2, 0);

        contenedor.getChildren().addAll(titulo, grid);
        return contenedor;
    }

    private Node construirBloqueMetricasJuntas() {
        HBox fila = new HBox(30);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox scheduler = (VBox) construirMetricasScheduler();
        VBox memoria = (VBox) construirMetricasMemoria();

        scheduler.setMinWidth(480);
        memoria.setMinWidth(480);

        fila.getChildren().addAll(scheduler, memoria);
        return fila;
    }

    private Node construirMetricasScheduler() {
        VBox contenedor = new VBox(10);
        Label titulo = new Label("Métricas del Scheduler");
        titulo.getStyleClass().add("section-title");

        GridPane grid = crearGridMetricas();
        grid.add(crearTarjeta("Procesos Completados", procesosCompletadosLabel), 0, 0);
        grid.add(crearTarjeta("Tiempo Promedio de Respuesta", tiempoRespuestaLabel), 1, 0);
        grid.add(crearTarjeta("Cambios de Contexto", cambiosContextoLabel), 2, 0);
        grid.add(crearTarjeta("Tiempo Total de CPU", tiempoCpuLabel), 3, 0);
        grid.add(crearTarjeta("Tiempo Inactivo", tiempoOciosoLabel), 4, 0);

        contenedor.getChildren().addAll(titulo, grid);
        return contenedor;
    }

    private Node construirMetricasMemoria() {
        VBox contenedor = new VBox(10);
        Label titulo = new Label("Métricas de Memoria");
        titulo.getStyleClass().add("section-title");

        GridPane grid = crearGridMetricas();
        grid.add(crearTarjeta("Cargas Totales", cargasTotalesLabel), 0, 0);
        grid.add(crearTarjeta("Fallos de Página", fallosPaginaLabel), 1, 0);
        grid.add(crearTarjeta("Reemplazos Totales", reemplazosLabel), 2, 0);
        grid.add(crearTarjeta("Marcos Libres", marcosLibresLabel), 3, 0);

        contenedor.getChildren().addAll(titulo, grid);
        return contenedor;
    }

    private void construirVisualizaciones() {
        Label subtitulo = new Label("Visualizaciones");
        subtitulo.getStyleClass().add("section-title");

        HBox graficas = new HBox(14);
        graficas.setAlignment(Pos.CENTER_LEFT);

        VBox graficaCpuCard = crearContenedorGrafica("Uso de CPU");
        graficaCpu.setMinSize(120, 120);
        graficaCpu.setPrefSize(120, 120);
        graficaCpu.setMaxSize(120, 120);
        graficaCpu.setStyle("-fx-progress-color: #135bec;");

        Label etiquetaCpu = new Label("Ocupación");
        etiquetaCpu.getStyleClass().add("chart-label");
        estadoCpu.getStyleClass().add("chart-helper");

        VBox datosCpu = new VBox(4, etiquetaCpu, estadoCpu);
        datosCpu.setAlignment(Pos.CENTER);

        BorderPane cpuPane = new BorderPane();
        cpuPane.setCenter(graficaCpu);
        cpuPane.setBottom(datosCpu);
        BorderPane.setAlignment(datosCpu, Pos.CENTER);

        graficaCpuCard.getChildren().add(cpuPane);

        VBox graficaEspera = crearContenedorGrafica("Tiempo de espera por proceso");
        contenedorBarras.setFillWidth(true);
        graficaEspera.getChildren().add(contenedorBarras);

        graficas.getChildren().addAll(graficaCpuCard, graficaEspera);

        content.getChildren().addAll(subtitulo, graficas);  // <-- antes era getChildren()
    }

    private void construirTabla() {
        Label titulo = new Label("Métricas por proceso");
        titulo.getStyleClass().add("section-title");

        Label labelProceso = new Label("Proceso");
        Label labelEspera = new Label("Espera (ms)");
        Label labelRetorno = new Label("Retorno (ms)");
        Label labelRespuesta = new Label("Respuesta (ms)");
        Label labelFallos = new Label("Fallos página");

        labelProceso.getStyleClass().add("text-clear");
        labelEspera.getStyleClass().add("text-clear");
        labelRetorno.getStyleClass().add("text-clear");
        labelRespuesta.getStyleClass().add("text-clear");
        labelFallos.getStyleClass().add("text-clear");

        TableColumn<ResultadoProceso, String> pidCol = new TableColumn<>();
        pidCol.setGraphic(labelProceso);
        pidCol.setCellValueFactory(new PropertyValueFactory<>("pid"));

        TableColumn<ResultadoProceso, Integer> esperaCol = new TableColumn<>();
        esperaCol.setGraphic(labelEspera);
        esperaCol.setCellValueFactory(new PropertyValueFactory<>("tiempoEspera"));

        TableColumn<ResultadoProceso, Integer> retornoCol = new TableColumn<>();
        retornoCol.setGraphic(labelRetorno);
        retornoCol.setCellValueFactory(new PropertyValueFactory<>("tiempoRetorno"));

        TableColumn<ResultadoProceso, Integer> respuestaCol = new TableColumn<>();
        respuestaCol.setGraphic(labelRespuesta);
        respuestaCol.setCellValueFactory(new PropertyValueFactory<>("tiempoRespuesta"));

        TableColumn<ResultadoProceso, Integer> fallosCol = new TableColumn<>();
        fallosCol.setGraphic(labelFallos);
        fallosCol.setCellValueFactory(new PropertyValueFactory<>("fallosPagina"));

        tablaProcesos.getColumns().addAll(pidCol, esperaCol, retornoCol, respuestaCol, fallosCol);
        tablaProcesos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaProcesos.setPrefHeight(260);
        tablaProcesos.getStyleClass().add("result-table");
        tablaProcesos.setStyle("-fx-background-color: #0f0a1a;");

        VBox.setVgrow(tablaProcesos, Priority.ALWAYS);
        content.getChildren().addAll(titulo, tablaProcesos);   // <-- antes era getChildren()
    }

    public void actualizarDatos(DatosResultados datos) {
        algPlanLabel.setText(datos.getAlgPlanificacion());
        algMemLabel.setText(datos.getAlgMemoria());
        totalProcesosLabel.setText(String.valueOf(datos.getTotalProcesos()));

        procesosCompletadosLabel.setText(String.format("%d / %d", datos.getProcesosCompletados(), datos.getTotalProcesos()));
        tiempoRespuestaLabel.setText(String.format("%.1f ms", datos.getTiempoRespuestaPromedio()));
        cambiosContextoLabel.setText(String.valueOf(datos.getCambiosContexto()));
        tiempoCpuLabel.setText(String.format("%d ms", datos.getTiempoCpu()));
        tiempoOciosoLabel.setText(String.format("%d ms", datos.getTiempoOcioso()));

        cargasTotalesLabel.setText(String.valueOf(datos.getCargasTotales()));
        fallosPaginaLabel.setText(String.valueOf(datos.getFallosPagina()));
        reemplazosLabel.setText(String.valueOf(datos.getReemplazosPagina()));
        marcosLibresLabel.setText(String.format("%d / %d", datos.getMarcosLibres(), datos.getMarcosTotales()));

        double progresoCpu = Math.min(1.0, Math.max(0, datos.getUsoCpu() / 100));
        graficaCpu.setProgress(progresoCpu);
        porcentajeCpuLabel.setText(String.format("%.0f%%", datos.getUsoCpu()));
        estadoCpu.setText(String.format("Trabajo: %.1f%%  |  Ocioso: %.1f%%", datos.getUsoCpu(), datos.getOcioCpu()));

        tablaProcesos.getItems().setAll(datos.getResumenProcesos());
        actualizarBarras(datos);
    }

    private void actualizarBarras(DatosResultados datos) {
        contenedorBarras.getChildren().clear();
        double maxEspera = datos.getResumenProcesos().stream()
                .mapToDouble(ResultadoProceso::getTiempoEspera)
                .max()
                .orElse(1);

        for (ResultadoProceso proceso : datos.getResumenProcesos()) {
            Label pid = new Label(proceso.getPid());
            pid.getStyleClass().add("chart-label");

            ProgressBar barra = new ProgressBar();
            barra.setProgress(proceso.getTiempoEspera() / maxEspera);
            barra.setPrefWidth(320);
            barra.setStyle("-fx-accent: #135bec;");

            Label valor = new Label(proceso.getTiempoEspera() + " ms");
            valor.getStyleClass().add("chart-helper");

            HBox fila = new HBox(10, pid, barra, valor);
            fila.setAlignment(Pos.CENTER_LEFT);
            contenedorBarras.getChildren().add(fila);
        }
    }

    private VBox crearTarjeta(String titulo, Label valor) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("card-subtitle");

        VBox tarjeta = new VBox(6);
        tarjeta.getStyleClass().add("card");
        tarjeta.getChildren().addAll(etiqueta, valor);
        return tarjeta;
    }

    private VBox crearDatoResumido(String titulo, Label valor) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("card-subtitle");
        valor.getStyleClass().add("metric-value");

        VBox caja = new VBox(4, etiqueta, valor);
        caja.getStyleClass().add("card");
        return caja;
    }

    private VBox crearContenedorGrafica(String titulo) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("card-subtitle");

        VBox contenedor = new VBox(10);
        contenedor.getStyleClass().add("card");
        contenedor.getChildren().add(etiqueta);
        return contenedor;
    }

    private Label crearValorPrincipal() {
        Label label = new Label("—");
        label.getStyleClass().add("metric-value");
        return label;
    }

    private GridPane crearGridMetricas() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        return grid;
    }
}
