package grupo5.cliente.puertos;

import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.EntidadPerfilVista;
import java.util.UUID;

/** Mi perfil de la entidad: sus datos, sus medios y la constancia de los legales. */
public interface EntidadPerfilPuerto {

  /** Cómo se llama en el sitio a quien recibe (R11-45). */
  String ORGANIZACION = "Organización";

  EntidadPerfilVista perfil(UUID entidad);

  /** La cuenta del menú y del saludo (R11-44): la razón social de la organización. */
  default CuentaVista cuenta(UUID entidad) {
    return new CuentaVista(perfil(entidad).nombre(), ORGANIZACION);
  }
}
