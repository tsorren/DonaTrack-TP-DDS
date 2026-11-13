package grupo5.cliente.puertos;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Qué cuenta de administración usa el sitio (R11-10): fuera de la maqueta, el {@code sub} del token
 * de la sesión; con la maqueta, la de ejemplo. Nada que venga en el pedido la cambia.
 */
public interface AdministradorActual {

  /** El id de la cuenta de administración, o vacío si la sesión no es de administración. */
  Optional<String> cuenta(HttpServletRequest pedido);
}
