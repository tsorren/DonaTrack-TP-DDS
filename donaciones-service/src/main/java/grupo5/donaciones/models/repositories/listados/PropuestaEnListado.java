package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import java.util.Map;
import java.util.UUID;

/**
 * Una propuesta de una página del listado, con lo que se calcula contra las otras pendientes y el
 * depósito (R11-24): su situación (null si ya se decidió), cuántas otras pendientes cubren la misma
 * necesidad y, por cada donación que pide, lo que queda en el depósito y lo que piden de ella todas
 * las pendientes.
 */
public record PropuestaEnListado(
    Propuesta propuesta,
    SituacionPropuesta situacion,
    int otrasPendientesMismaNecesidad,
    Map<UUID, StockEnPropuestas> porDonacion) {

  public PropuestaEnListado {
    porDonacion = Map.copyOf(porDonacion);
  }
}
