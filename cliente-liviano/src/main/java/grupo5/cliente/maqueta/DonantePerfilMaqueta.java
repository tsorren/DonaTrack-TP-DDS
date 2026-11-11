package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** El perfil de la empresa de ejemplo, el que dibujaba la plantilla. Guardar no cambia nada. */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonantePerfilMaqueta implements DonantePerfilPuerto {

  private static final String CIUDAD = "Ciudad de Buenos Aires";

  private static final DonantePerfilVista PERFIL =
      new DonantePerfilVista(
          "Distribuidora Ejemplo SA",
          "Empresa donante · contacto@distribuidora-ejemplo.test",
          "Empresa donante · Sostenedor",
          false,
          "Distribución de alimentos",
          "30-00000000-0",
          "Avenida Ejemplo",
          "1234",
          "3.º B",
          "C1000",
          CIUDAD,
          CIUDAD,
          "contacto@distribuidora-ejemplo.test",
          "11 5555-0100",
          null,
          "Aceptaste la versión 1 el 1 de noviembre de 2026.");

  @Override
  public DonantePerfilVista perfil(UUID donante) {
    return PERFIL;
  }

  @Override
  public void mostrarNombre(UUID donante, boolean mostrar) {
    // La maqueta no guarda: el formulario lo atiende SitioController.
  }
}
