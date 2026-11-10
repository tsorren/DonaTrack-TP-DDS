package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * En la maqueta con las APIs, la cuenta llega por {@code ?donante=<id>} o {@code ?entidad=<id>} y
 * la sesión la recuerda (decisión E1 del plan del cliente con datos reales); con ingreso, la cuenta
 * sale del token. Un id mal escrito no cambia la cuenta. No controla pertenencia: eso llega con la
 * etapa 2 del gateway (F5).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Profile("maqueta")
@Primary
@ConditionalOnProperty(name = "cliente.datos.cuenta", havingValue = "parametro")
class CuentaPorParametro implements CuentaActual {

  static final String PARAMETRO = "donante";
  static final String PARAMETRO_ENTIDAD = "entidad";

  @Override
  public Optional<UUID> donante(HttpServletRequest pedido) {
    return cuenta(pedido, PARAMETRO);
  }

  @Override
  public Optional<UUID> entidad(HttpServletRequest pedido) {
    return cuenta(pedido, PARAMETRO_ENTIDAD);
  }

  private static Optional<UUID> cuenta(HttpServletRequest pedido, String parametro) {
    HttpSession sesion = pedido.getSession();
    String clave = "cuenta." + parametro;
    String dado = pedido.getParameter(parametro);
    if (dado != null) {
      try {
        sesion.setAttribute(clave, UUID.fromString(dado));
      } catch (IllegalArgumentException malEscrito) {
        // Se queda con la cuenta que ya tenía la sesión, si tenía.
      }
    }
    return Optional.ofNullable((UUID) sesion.getAttribute(clave));
  }
}
