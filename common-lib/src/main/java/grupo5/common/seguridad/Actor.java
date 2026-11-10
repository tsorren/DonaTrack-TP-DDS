package grupo5.common.seguridad;

import java.util.UUID;

/** Quien hace la llamada, armado desde el token por {@link Actores}. */
public sealed interface Actor permits Administrador, Miembro {

  boolean puedeOperarSobre(UUID personaId);

  boolean tiene(Permiso permiso);
}
