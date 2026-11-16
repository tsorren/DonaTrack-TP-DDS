package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * La cuenta de ejemplo: siempre el mismo donante y la misma entidad, que los adaptadores de la
 * maqueta ignoran.
 */
@Component
@Profile("maqueta")
@ConditionalOnProperty(
    name = "cliente.datos.cuenta",
    havingValue = "maqueta",
    matchIfMissing = true)
class CuentaMaqueta implements CuentaActual {

  static final UUID DONANTE = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final UUID ENTIDAD = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Override
  public Optional<UUID> donante(HttpServletRequest pedido) {
    return Optional.of(DONANTE);
  }

  @Override
  public Optional<UUID> entidad(HttpServletRequest pedido) {
    return Optional.of(ENTIDAD);
  }
}
