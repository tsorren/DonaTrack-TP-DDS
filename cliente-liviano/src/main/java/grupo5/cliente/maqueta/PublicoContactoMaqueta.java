package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.PublicoContactoPuerto;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Contacto en la maqueta: el formulario no lleva clave (el envío lo atiende {@code SitioController}
 * y no guarda) y la pantalla de enviado muestra el número de ejemplo.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.contacto",
    havingValue = "maqueta",
    matchIfMissing = true)
class PublicoContactoMaqueta implements PublicoContactoPuerto {

  private static final long NUMERO = 4821;

  @Override
  public UUID nuevaClave() {
    return null;
  }

  @Override
  public Long numeroDeEjemplo() {
    return NUMERO;
  }

  @Override
  public long enviar(Mensaje mensaje, String ipOrigen) {
    return NUMERO;
  }
}
