package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.DatosResultados;
import model.ResultadoProceso;

public class ResultadosPage extends VBox {

    private final Label valorEspera = crearValorPrincipal();
    private final Label valorRetorno = crearValorPrincipal();
    private final Label valorCpu = crearValorPrincipal();
    private final Label valorFallos = crearValorPrincipal();
    private final Label valorReemplazos = crearValorPrincipal();
    private final ProgressIndicator graficaCpu = new ProgressIndicator();
    private final Label estadoCpu = new Label();
    private final VBox contenedorBarras = new VBox(8);
    private final TableView<ResultadoProceso> tablaProcesos = new TableView<>();

    public ResultadosPage() {
    }

    public ResultadosPage(DatosResultados datos) {
        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("page-container");
        construirEncabezado();
        construirTarjetas();
        construirVisualizaciones();
        construirTabla();
        actualizarDatos(datos);
    }

    private void construirEncabezado() {
        BorderPane barra = new BorderPane();
        barra.setPadding(new Insets(0, 0, 8, 0));

        VBox textos = new VBox(4);
        Label titulo = new Label("Resultados de Simulación");
        titulo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Métricas detalladas de la última ejecución.");
        subtitulo.getStyleClass().add("page-subtitle");
        textos.getChildren().addAll(titulo, subtitulo);

        Button exportar = new Button("Exportar resultados");
        exportar.getStyleClass().add("secondary-button");

        barra.setLeft(textos);
        barra.setRight(exportar);
        setMargin(barra, new Insets(0, 0, 8, 0));
        getChildren().add(barra);
    }

    private void construirTarjetas() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(crearTarjeta("Tiempo de espera promedio", valorEspera), 0, 0);
        grid.add(crearTarjeta("Tiempo de retorno promedio", valorRetorno), 1, 0);
        grid.add(crearTarjeta("Utilización de CPU", valorCpu), 2, 0);
        grid.add(crearTarjeta("Fallos de página", valorFallos), 3, 0);
        grid.add(crearTarjeta("Reemplazos", valorReemplazos), 4, 0);

        getChildren().add(grid);
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

        getChildren().addAll(subtitulo, graficas);
    }

    private void construirTabla() {
        Label titulo = new Label("Métricas por proceso");
        titulo.getStyleClass().add("section-title");

        TableColumn<ResultadoProceso, String> pidCol = new TableColumn<>("Proceso");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("pid"));

        TableColumn<ResultadoProceso, Integer> esperaCol = new TableColumn<>("Espera (ms)");
        esperaCol.setCellValueFactory(new PropertyValueFactory<>("tiempoEspera"));

        TableColumn<ResultadoProceso, Integer> retornoCol = new TableColumn<>("Retorno (ms)");
        retornoCol.setCellValueFactory(new PropertyValueFactory<>("tiempoRetorno"));

        TableColumn<ResultadoProceso, Integer> respuestaCol = new TableColumn<>("Respuesta (ms)");
        respuestaCol.setCellValueFactory(new PropertyValueFactory<>("tiempoRespuesta"));

        TableColumn<ResultadoProceso, Integer> fallosCol = new TableColumn<>("Fallos página");
        fallosCol.setCellValueFactory(new PropertyValueFactory<>("fallosPagina"));

        TableColumn<ResultadoProceso, Integer> reemplazosCol = new TableColumn<>("Reemplazos");
        reemplazosCol.setCellValueFactory(new PropertyValueFactory<>("reemplazos"));

        tablaProcesos.getColumns().addAll(pidCol, esperaCol, retornoCol, respuestaCol, fallosCol, reemplazosCol);
        tablaProcesos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaProcesos.setPrefHeight(260);
        tablaProcesos.getStyleClass().add("result-table");

        VBox.setVgrow(tablaProcesos, Priority.ALWAYS);
        getChildren().addAll(titulo, tablaProcesos);
    }

    public void actualizarDatos(DatosResultados datos) {
        valorEspera.setText(String.format("%.1f ms", datos.getTiempoEsperaPromedio()));
        valorRetorno.setText(String.format("%.1f ms", datos.getTiempoRetornoPromedio()));
        valorCpu.setText(String.format("%.1f%%", datos.getUsoCpu()));
        valorFallos.setText(String.valueOf(datos.getFallosPagina()));
        valorReemplazos.setText(String.valueOf(datos.getReemplazosPagina()));

        double progresoCpu = Math.min(1.0, Math.max(0, datos.getUsoCpu() / 100));
        graficaCpu.setProgress(progresoCpu);
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
}
