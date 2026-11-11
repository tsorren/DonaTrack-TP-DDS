package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.EntidadPerfilPuerto;
import grupo5.cliente.vistas.EntidadPerfilVista;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** El perfil del Comedor Barrio Ejemplo, que representa Marta. */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class EntidadPerfilMaqueta implements EntidadPerfilPuerto {

  private static final String CORREO = "comedor@ejemplo.org";

  private static final EntidadPerfilVista PERFIL =
      new EntidadPerfilVista(
          "Comedor Barrio Ejemplo",
          "Comedor · Marta, " + CORREO,
          "Comedor",
          "asociacion",
          "Calle Ejemplo 450, Localidad Ejemplo, Buenos Aires",
          "Marta Ejemplo",
          CORREO,
          CORREO,
          "11 5555-0200",
          null,
          "correo",
          "Aceptaste la versión 1 el 1 de noviembre de 2026.");

  @Override
  public EntidadPerfilVista perfil(UUID entidad) {
    return PERFIL;
  }
}
