package grupo5.cliente.seguridad;

import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * La cuenta es la de la sesión: el donante es la persona que dice su token, y la entidad, la
 * persona del token cuando su área es la de entidad (Donaciones la compara igual, con {@code
 * personaId}). Nada que venga en el pedido la cambia.
 */
@Component
@Profile("!maqueta")
class CuentaDelToken implements CuentaActual {

  @Override
  public Optional<UUID> donante(HttpServletRequest pedido) {
    return usuario().map(UsuarioActual::personaId);
  }

  @Override
  public Optional<UUID> entidad(HttpServletRequest pedido) {
    return usuario()
        .filter(usuario -> usuario.area().filter(Area.ENTIDAD::equals).isPresent())
        .map(UsuarioActual::personaId);
  }

  private static Optional<UsuarioActual> usuario() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .filter(OidcUser.class::isInstance)
        .map(principal -> UsuarioActual.desde(((OidcUser) principal).getClaims()));
  }
}
