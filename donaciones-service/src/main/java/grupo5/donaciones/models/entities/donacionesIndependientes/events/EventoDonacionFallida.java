package grupo5.donaciones.models.entities.donacionesIndependientes.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventoDonacionFallida extends EventoDonacionIndependiente {
  private final UUID idNecesidad;
  private final String justificacion;
  private final Boolean replanificable;

  public EventoDonacionFallida(
      UUID donacionIndependienteId,
      UUID donacionOriginalId,
      UUID idNecesidad,
      String justificacion,
      Boolean replanificable) {
    super(donacionIndependienteId, donacionOriginalId);
    this.idNecesidad = idNecesidad;
    this.justificacion = justificacion;
    this.replanificable = replanificable;
  }

  public UUID idNecesidad() {
    return idNecesidad;
  }

  public String justificacion() {
    return justificacion;
  }

  public Boolean replanificable() {
    return replanificable;
  }
}
