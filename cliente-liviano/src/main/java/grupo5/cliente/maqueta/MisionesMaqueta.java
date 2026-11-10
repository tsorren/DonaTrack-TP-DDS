package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.MisionesPuerto;
import grupo5.cliente.vistas.MisionesVista;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador de maqueta para misiones e insignias. En modo maqueta, delega el contenido en la
 * plantilla (con th:with) e interceptor CuentaDeEjemplo.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.misiones",
    havingValue = "maqueta",
    matchIfMissing = true)
class MisionesMaqueta implements MisionesPuerto {

  @Override
  public MisionesVista misiones(UUID donanteId) {
    return new MisionesVista(
        false, Escenario.actual() == Escenario.VACIO, null, List.of(), List.of(), List.of());
  }
}
