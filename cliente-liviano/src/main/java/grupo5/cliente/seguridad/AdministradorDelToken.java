package grupo5.cliente.seguridad;

import grupo5.cliente.puertos.AdministradorActual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** La cuenta de administración es la de la sesión: el {@code sub} de su token, si es del área. */
@Component
@Profile("!maqueta")
class AdministradorDelToken implements AdministradorActual {

  @Override
  public Optional<String> cuenta(HttpServletRequest pedido) {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .filter(OidcUser.class::isInstance)
        .map(OidcUser.class::cast)
        .filter(
            usuario ->
                UsuarioActual.desde(usuario.getClaims())
                    .area()
                    .filter(Area.ADMIN::equals)
                    .isPresent())
        .map(OidcUser::getSubject);
  }
}
