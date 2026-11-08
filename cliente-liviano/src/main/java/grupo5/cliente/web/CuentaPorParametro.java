package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Sin login todavía, el donante llega por {@code ?donante=<id>} y la sesión lo recuerda (decisión
 * E1 del plan del cliente con datos reales). Un id mal escrito no cambia la cuenta. No controla
 * pertenencia: eso llega con la etapa 2 del gateway (F5).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.cuenta", havingValue = "parametro")
class CuentaPorParametro implements CuentaActual {

  static final String PARAMETRO = "donante";
  private static final String CLAVE = "cuenta.donante";

  @Override
  public Optional<UUID> donante(HttpServletRequest pedido) {
    HttpSession sesion = pedido.getSession();
    String dado = pedido.getParameter(PARAMETRO);
    if (dado != null) {
      try {
        sesion.setAttribute(CLAVE, UUID.fromString(dado));
      } catch (IllegalArgumentException malEscrito) {
        // Se queda con la cuenta que ya tenía la sesión, si tenía.
      }
    }
    return Optional.ofNullable((UUID) sesion.getAttribute(CLAVE));
  }
}
