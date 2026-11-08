package grupo5.cliente.seguridad;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Lo que el ID token dice de quien ingresó. Es el único lugar del cliente que conoce los nombres de
 * los claims. {@code personaId} es nulo mientras la cuenta no completó su perfil, y {@code
 * solicitudEntidad} lo trae la cuenta cuya entidad espera la aprobación del equipo.
 */
public record UsuarioActual(
    Set<String> roles, Set<String> permisos, UUID personaId, String solicitudEntidad) {

  public static UsuarioActual desde(Map<String, Object> claims) {
    Object personaId = claims.get("personaId");
    return new UsuarioActual(
        valores(claims, "roles"),
        valores(claims, "permisos"),
        personaId instanceof String id ? UUID.fromString(id) : null,
        claims.get("solicitudEntidad") instanceof String solicitud ? solicitud : null);
  }

  public EstadoDeCuenta estado() {
    if (personaId == null && area().map(Area::requierePersona).orElse(true)) {
      return EstadoDeCuenta.SIN_PERFIL;
    }
    return solicitudEntidad == null ? EstadoDeCuenta.ACTIVA : EstadoDeCuenta.SOLICITUD_RECIBIDA;
  }

  public Optional<Area> area() {
    return Area.de(roles);
  }

  /** Los roles quedan como {@code ROLE_*} para {@code hasRole}; los permisos, tal cual. */
  public Collection<GrantedAuthority> autoridades() {
    return Stream.concat(roles.stream().map("ROLE_"::concat), permisos.stream())
        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
        .toList();
  }

  private static Set<String> valores(Map<String, Object> claims, String claim) {
    return claims.get(claim) instanceof Collection<?> valores
        ? valores.stream().map(String::valueOf).collect(toUnmodifiableSet())
        : Set.of();
  }
}
