package grupo5.donaciones.fixtures;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import grupo5.donaciones.models.ports.CargadorDonantes;
import grupo5.donaciones.models.ports.CargadorDonantes.FilaPadron;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/** Hace que un doble de {@link CargadorDonantes} entregue un padrón como lo haría el lector. */
public final class PadronFixtures {

  private PadronFixtures() {}

  /** Cuenta las filas y las entrega de a tandas del tamaño que pida el importador. */
  public static void entregar(CargadorDonantes lector, List<FilaPadron> filas) {
    when(lector.contarFilas(any(InputStream.class))).thenReturn(filas.size());
    doAnswer(
            pedido -> {
              int tamanio = pedido.getArgument(1);
              Consumer<List<FilaPadron>> alLeerTanda = pedido.getArgument(2);
              for (int desde = 0; desde < filas.size(); desde += tamanio) {
                alLeerTanda.accept(filas.subList(desde, Math.min(desde + tamanio, filas.size())));
              }
              return null;
            })
        .when(lector)
        .leerPorTandas(any(InputStream.class), anyInt(), any());
  }

  public static void entregar(CargadorDonantes lector, FilaPadron... filas) {
    entregar(lector, List.of(filas));
  }
}
