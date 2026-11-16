package grupo5.cliente.maqueta;

import java.util.Arrays;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Estado que muestran los adaptadores de ejemplo, elegido con {@code ?escenario=<nombre>} (por
 * ejemplo {@code ?escenario=vacio}). Sin parámetro, hay datos.
 */
public enum Escenario {
  CON_DATOS,
  VACIO,
  LENTO,
  ERROR,
  SIN_PERMISO;

  public static Escenario actual() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido) {
      return de(pedido.getRequest().getParameter("escenario"));
    }
    return CON_DATOS;
  }

  static Escenario de(String valor) {
    if (valor == null || valor.isBlank()) {
      return CON_DATOS;
    }
    return Arrays.stream(values())
        .filter(escenario -> escenario.parametro().equals(valor))
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Escenario desconocido: " + valor));
  }

  /** Nombre en la URL: {@code sin-permiso}. */
  public String parametro() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
