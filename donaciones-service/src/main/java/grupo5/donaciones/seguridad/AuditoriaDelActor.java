package grupo5.donaciones.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quién hace una acción, para la auditoría (RI-SVC-04): con token es el {@code sub} de la cuenta,
 * que el servicio ya validó; sin token (seguridad apagada) vale el encabezado {@code X-Actor}, que
 * queda como transición y deja de hacer falta cuando la seguridad sea obligatoria.
 */
public final class AuditoriaDelActor {

  private AuditoriaDelActor() {}

  public static String de(Authentication autenticacion, String actorLegado) {
    if (autenticacion != null
        && autenticacion.getCredentials() instanceof Jwt token
        && token.getSubject() != null
        && !token.getSubject().isBlank()) {
      return token.getSubject();
    }
    if (actorLegado == null || actorLegado.isBlank()) {
      throw new IllegalArgumentException("El encabezado 'X-Actor' es obligatorio.");
    }
    return actorLegado;
  }
}
