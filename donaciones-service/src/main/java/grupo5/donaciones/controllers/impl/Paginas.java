package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.dto.Pagina;
import java.util.List;
import org.springframework.http.ResponseEntity;

/** La respuesta de un listado paginado: el arreglo en el cuerpo y el total en X-Total-Count. */
final class Paginas {

  static final String HEADER_TOTAL = "X-Total-Count";

  private Paginas() {}

  static <T> ResponseEntity<List<T>> conTotal(Pagina<T> pagina) {
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(pagina.total()))
        .body(pagina.elementos());
  }
}
