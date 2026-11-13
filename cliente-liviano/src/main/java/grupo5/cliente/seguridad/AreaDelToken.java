package grupo5.cliente.seguridad;

import grupo5.cliente.puertos.AreaDeLaSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * El área de la sesión es la del token, solo si la cuenta está activa (con su perfil completo y sin
 * solicitud pendiente) o, para {@link #pendiente}, solo si la solicitud de su entidad espera la
 * aprobación: ni la dirección pedida ni ningún dato del pedido la cambian.
 */
@Component
@Profile("!maqueta")
class AreaDelToken implements AreaDeLaSesion {

  @Override
  public Optional<Area> activa(HttpServletRequest pedido) {
    return areaSi(EstadoDeCuenta.ACTIVA);
  }

  @Override
  public Optional<Area> pendiente(HttpServletRequest pedido) {
    return areaSi(EstadoDeCuenta.SOLICITUD_RECIBIDA);
  }

  private static Optional<Area> areaSi(EstadoDeCuenta estado) {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .filter(OidcUser.class::isInstance)
        .map(principal -> UsuarioActual.desde(((OidcUser) principal).getClaims()))
        .filter(usuario -> usuario.estado() == estado)
        .flatMap(UsuarioActual::area);
  }
}
