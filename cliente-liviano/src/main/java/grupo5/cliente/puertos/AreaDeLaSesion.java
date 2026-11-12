package grupo5.cliente.puertos;

import grupo5.cliente.seguridad.Area;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * De quién es la sesión que mira una página que no es de ningún área: las páginas de error y
 * Contacto (docs/interfaz/flujos/raices-paginas-de-error.md). Si hay una cuenta con su perfil
 * completo y sin solicitud pendiente, dice su área y la página se arma con el marco privado; si no,
 * queda vacío y la página usa el marco público. Fuera de {@code maqueta} lo dice el token y no pide
 * nada a ningún servicio.
 */
@FunctionalInterface
public interface AreaDeLaSesion {

  /** El área de la cuenta con la sesión iniciada y activa; vacío si no la hay. */
  Optional<Area> activa(HttpServletRequest pedido);
}
