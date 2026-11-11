package grupo5.cliente.puertos;

import grupo5.cliente.vistas.EntidadPerfilVista;
import java.util.UUID;

/** Mi perfil de la entidad: sus datos, sus medios y la constancia de los legales. */
public interface EntidadPerfilPuerto {

  EntidadPerfilVista perfil(UUID entidad);
}
