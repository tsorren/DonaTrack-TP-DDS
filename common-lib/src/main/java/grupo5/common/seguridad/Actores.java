package grupo5.common.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/** Único lugar que conoce los claims del token: arma el {@link Actor} y sus autoridades. */
public final class Actores {

  private static final String ROLES = "roles";
  private static final String PERMISOS = "permisos";
  private static final String PERSONA_ID = "personaId";
  private static final String ADMIN = "ADMIN";

  private Actores() {}

  public static Actor desde(Jwt token) {
    return valores(token, ROLES).contains(ADMIN)
        ? new Administrador()
        : new Miembro(personaId(token), Set.copyOf(valores(token, PERMISOS)));
  }

  /** Los roles quedan como {@code ROLE_*} para {@code hasRole}; los permisos, tal cual. */
  public static Collection<GrantedAuthority> autoridades(Jwt token) {
    Stream<String> roles = valores(token, ROLES).stream().map("ROLE_"::concat);
    return Stream.concat(roles, valores(token, PERMISOS).stream())
        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
        .toList();
  }

  /** Autenticación con el {@link Actor} como principal y el token como credencial. */
  static PreAuthenticatedAuthenticationToken autenticacion(Jwt token) {
    return new PreAuthenticatedAuthenticationToken(desde(token), token, autoridades(token));
  }

  private static List<String> valores(Jwt token, String claim) {
    return Objects.requireNonNullElse(token.getClaimAsStringList(claim), List.of());
  }

  private static UUID personaId(Jwt token) {
    String personaId = token.getClaimAsString(PERSONA_ID);
    return personaId == null ? null : UUID.fromString(personaId);
  }
}
