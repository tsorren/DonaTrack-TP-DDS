package grupo5.common.seguridad;

import java.util.UUID;

/** Admin de plataforma: tiene todos los permisos y opera sobre cualquier organización. */
public record Administrador() implements Actor {

  @Override
  public boolean puedeOperarSobre(UUID personaId) {
    return true;
  }

  @Override
  public boolean tiene(Permiso permiso) {
    return true;
  }
}
