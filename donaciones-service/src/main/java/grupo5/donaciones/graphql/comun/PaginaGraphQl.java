package grupo5.donaciones.graphql.comun;

import grupo5.donaciones.dto.Pagina;
import java.util.List;
import java.util.function.Function;

/**
 * Los tipos de página del esquema ({@code PaginaNecesidades}, {@code PaginaEntidades} y {@code
 * PaginaDonaciones}): los mismos cuatro campos para las tres raíces.
 *
 * @param elementos los de esta página, ya mapeados al tipo del esquema
 * @param total cuántos cumplen el filtro, en todas las páginas
 * @param pagina la pedida, desde 0
 * @param tamanio el tamaño pedido
 */
public record PaginaGraphQl<T>(List<T> elementos, int total, int pagina, int tamanio) {

  public PaginaGraphQl {
    elementos = List.copyOf(elementos);
  }

  /** Arma la página de GraphQL a partir de la del servicio, mapeando cada elemento. */
  public static <D, T> PaginaGraphQl<T> de(
      Pagina<D> pagina, PedidoDePagina pedido, Function<? super D, ? extends T> mapeo) {
    List<T> elementos = pagina.elementos().stream().<T>map(mapeo).toList();
    return new PaginaGraphQl<>(elementos, pagina.total(), pedido.pagina(), pedido.tamanio());
  }
}
