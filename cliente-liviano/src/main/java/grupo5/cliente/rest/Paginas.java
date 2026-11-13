package grupo5.cliente.rest;

import grupo5.cliente.vistas.Tramo;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import org.springframework.http.ResponseEntity;

/**
 * Pedir a un servicio una página de un listado (R11-01): el cuerpo trae las filas y {@code
 * X-Total-Count}, cuántas cumplen los filtros. Si la página pedida ya no existe (el total bajó o la
 * dirección se escribió a mano), se pide la última que hay: nunca una lista vacía cuando sí hay
 * filas.
 */
final class Paginas {

  static final String TOTAL = "X-Total-Count";

  private Paginas() {}

  /**
   * {@code pedido} recibe la página del servicio (desde 0) y la pide con su tamaño y sus filtros;
   * {@code pagina} es la de la pantalla (desde 1).
   */
  static <T> Tramo<T> pedir(int pagina, int tamanio, IntFunction<ResponseEntity<List<T>>> pedido) {
    int buscada = Math.max(1, pagina);
    ResponseEntity<List<T>> respuesta = pedido.apply(buscada - 1);
    List<T> filas = filas(respuesta);
    long total = total(respuesta, filas, buscada, tamanio);
    int paginas = Tramo.paginas(total, tamanio);
    if (filas.isEmpty() && total > 0 && buscada > paginas) {
      buscada = paginas;
      respuesta = pedido.apply(buscada - 1);
      filas = filas(respuesta);
      total = total(respuesta, filas, buscada, tamanio);
      paginas = Tramo.paginas(total, tamanio);
    }
    return new Tramo<>(filas, total, buscada, paginas);
  }

  private static <T> List<T> filas(ResponseEntity<List<T>> respuesta) {
    return Objects.requireNonNullElse(respuesta.getBody(), List.<T>of());
  }

  /** El total de la cabecera; sin ella, lo que se ve hasta esta página. */
  static long total(ResponseEntity<?> respuesta, List<?> filas, int pagina, int tamanio) {
    String total = respuesta.getHeaders().getFirst(TOTAL);
    if (total != null && total.strip().matches("\\d{1,18}")) {
      return Long.parseLong(total.strip());
    }
    return (long) (pagina - 1) * tamanio + filas.size();
  }
}
