package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import model.ResultadoProceso;

public class ResultadosPage extends VBox {

    public ResultadosPage() {
        setSpacing(20);
        setPadding(new Insets(20));

        construirEncabezado();
        construirTarjetas();
        construirVisualizaciones();
        construirTabla();
    }

    private void construirEncabezado() {
        BorderPane barra = new BorderPane();
        barra.setPadding(new Insets(0, 0, 8, 0));

        VBox textos = new VBox(4);
        Label titulo = new Label("Resultados de la Simulación");
        titulo.getStyleClass().add("page-title");

        Label subtitulo = new Label("Métricas");
        subtitulo.getStyleClass().add("page-subtitle");

        textos.getChildren().addAll(titulo, subtitulo);

        barra.setLeft(textos);
        getChildren().add(barra);
    }


    private void construirTarjetas() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(crearTarjeta("Tiempo de espera promedio", crearValorPrincipal()), 0, 0);
        grid.add(crearTarjeta("Tiempo de retorno promedio", crearValorPrincipal()), 1, 0);
        grid.add(crearTarjeta("Utilización de CPU", crearValorPrincipal()), 2, 0);
        grid.add(crearTarjeta("Fallos de página", crearValorPrincipal()), 3, 0);
        grid.add(crearTarjeta("Reemplazos", crearValorPrincipal()), 4, 0);

        getChildren().add(grid);
    }

    private VBox crearTarjeta(String titulo, Label valor) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("card-subtitle");

        VBox tarjeta = new VBox(6);
        tarjeta.getStyleClass().add("card");
        tarjeta.getChildren().addAll(etiqueta, valor);

        return tarjeta;
    }

    private Label crearValorPrincipal() {
        Label label = new Label("—");
        label.getStyleClass().add("metric-value");
        return label;
    }

    private VBox crearContenedorGrafica(String titulo) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("card-subtitle");

        VBox contenedor = new VBox(10);
        contenedor.getStyleClass().add("card");
        contenedor.getChildren().add(etiqueta);
        return contenedor;
    }

    private void construirVisualizaciones() {
        Label subtitulo = new Label("Visualizaciones");
        subtitulo.getStyleClass().add("section-title");

        HBox graficas = new HBox(14);
        graficas.setAlignment(Pos.CENTER_LEFT);

        VBox graficaCpuCard = crearContenedorGrafica("Uso de CPU");

        ProgressIndicator graficaCpu = new ProgressIndicator();
        graficaCpu.setPrefSize(120, 120);
        graficaCpu.setStyle("-fx-progress-color: #135bec;");

        Label etiquetaCpu = new Label("Ocupación");
        etiquetaCpu.getStyleClass().add("chart-label");

        Label estadoCpu = new Label("—");
        estadoCpu.getStyleClass().add("chart-helper");

        VBox datosCpu = new VBox(4, etiquetaCpu, estadoCpu);
        datosCpu.setAlignment(Pos.CENTER);

        BorderPane cpuPane = new BorderPane();
        cpuPane.setCenter(graficaCpu);
        cpuPane.setBottom(datosCpu);

        graficaCpuCard.getChildren().add(cpuPane);
        graficas.getChildren().add(graficaCpuCard);

        getChildren().addAll(subtitulo, graficas);
    }


    private void construirTabla() {
        Label titulo = new Label("Métricas por proceso");
        titulo.getStyleClass().add("section-title");

        TableView<ResultadoProceso> tabla = new TableView<>();

        TableColumn<ResultadoProceso, String> pidCol = new TableColumn<>("Proceso");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("pid"));

        TableColumn<ResultadoProceso, Integer> esperaCol = new TableColumn<>("Espera (ms)");
        esperaCol.setCellValueFactory(new PropertyValueFactory<>("tiempoEspera"));

        TableColumn<ResultadoProceso, Integer> retornoCol = new TableColumn<>("Retorno (ms)");
        retornoCol.setCellValueFactory(new PropertyValueFactory<>("tiempoRetorno"));
        tabla.getColumns().addAll(pidCol, esperaCol, retornoCol);

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getStyleClass().add("result-table");
        tabla.getStyleClass().add("result-table");

        getChildren().addAll(titulo, tabla);
    }

}

