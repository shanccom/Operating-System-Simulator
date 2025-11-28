package modules.gui.pages;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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

    private void construirVisualizaciones() {
        VBox seccion = new VBox(10);
        getChildren().add(seccion);
    }

    private void construirTabla() {
        TableView<ResultadoProceso> tabla = new TableView<>();
        getChildren().add(tabla);
    }
}

