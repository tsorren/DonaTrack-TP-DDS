package grupo5.cliente.seguridad;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/** Las áreas privadas del sitio, en orden de precedencia cuando una cuenta tiene más de un rol. */
public enum Area {
  ADMIN("admin", "Administración", false),
  ENTIDAD("entidad", "Entidad", true),
  DONANTE("donante", "Donante", true);

  private final String prefijo;
  private final String etiqueta;
  private final boolean requierePersona;

  Area(String prefijo, String etiqueta, boolean requierePersona) {
    this.prefijo = prefijo;
    this.etiqueta = etiqueta;
    this.requierePersona = requierePersona;
  }

  /** La cuenta de la plataforma no es una persona de Donaciones: no completa perfil. */
  public boolean requierePersona() {
    return requierePersona;
  }

  /** El prefijo de las pantallas del área: {@code donante-inicio}. */
  public String prefijo() {
    return prefijo;
  }

  public String etiqueta() {
    return etiqueta;
  }

  public String inicio() {
    return "/" + prefijo + "-inicio";
  }

  public static Optional<Area> deRuta(String prefijo) {
    return Arrays.stream(values()).filter(area -> area.prefijo.equals(prefijo)).findFirst();
  }

  /** El área de más precedencia entre los roles del token. */
  public static Optional<Area> de(Collection<String> roles) {
    return Arrays.stream(values()).filter(area -> roles.contains(area.name())).findFirst();
  }
}
