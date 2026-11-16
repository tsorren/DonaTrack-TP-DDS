package grupo5.donaciones.seguridad;

import grupo5.common.seguridad.Administrador;
import grupo5.donaciones.services.AdministradorQueActua;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * El {@code sub} del token de la llamada en curso, solo si es de ADMIN (el principal es un {@link
 * Administrador}, armado por common-lib desde el claim {@code roles}). La misma regla que el autor
 * de una propuesta (ADR del autor desde el token), en un solo lugar.
 */
@Component
class AdministradorDelToken implements AdministradorQueActua {

  @Override
  public Optional<String> cuenta() {
    Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacion != null
        && autenticacion.getPrincipal() instanceof Administrador
        && autenticacion.getCredentials() instanceof Jwt token
        && token.getSubject() != null
        && !token.getSubject().isBlank()) {
      return Optional.of(token.getSubject());
    }
    return Optional.empty();
  }
}
