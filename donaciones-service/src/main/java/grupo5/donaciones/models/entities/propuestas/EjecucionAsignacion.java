package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

@Getter
public class EjecucionAsignacion implements AggregateRoot {
  private final UUID id;
  private final LocalDateTime fechaEjecucion;
  private final Integer cantidadPropuestasGeneradas;

  public EjecucionAsignacion(
      UUID id, LocalDateTime fechaEjecucion, Integer cantidadPropuestasGeneradas) {
    if (cantidadPropuestasGeneradas == null || cantidadPropuestasGeneradas < 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    this.id = id != null ? id : UUID.randomUUID();
    this.fechaEjecucion =
        fechaEjecucion != null ? fechaEjecucion : LocalDateTime.now(ZoneId.of("UTC"));
    this.cantidadPropuestasGeneradas = cantidadPropuestasGeneradas;
  }

  public EjecucionAsignacion(Integer cantidadPropuestasGeneradas) {
    this(UUID.randomUUID(), LocalDateTime.now(ZoneId.of("UTC")), cantidadPropuestasGeneradas);
  }
}
