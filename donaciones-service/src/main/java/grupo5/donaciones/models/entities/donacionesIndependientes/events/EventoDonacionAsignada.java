package grupo5.donaciones.models.entities.donacionesIndependientes.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventoDonacionAsignada extends EventoDonacionIndependiente {
  private final UUID idNecesidad;

  public EventoDonacionAsignada(
      UUID donacionIndependienteId, UUID donacionOriginalId, UUID idNecesidad) {
    super(donacionIndependienteId, donacionOriginalId);
    this.idNecesidad = idNecesidad;
  }

  public UUID idNecesidad() {
    return idNecesidad;
  }
}
