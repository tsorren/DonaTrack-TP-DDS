package grupo5.common.seguridad;

import java.util.Set;
import java.util.UUID;

/** Persona que opera en nombre de {@code personaId}, con los permisos de su token. */
public record Miembro(UUID personaId, Set<String> permisos) implements Actor {

  @Override
  public boolean puedeOperarSobre(UUID duenio) {
    return duenio != null && duenio.equals(personaId);
  }

  @Override
  public boolean tiene(Permiso permiso) {
    return permisos.contains(permiso.name());
  }
}
