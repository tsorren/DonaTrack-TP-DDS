package grupo5.donaciones.models.entities.donacionesIndependientes.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventoDonacionVencida extends EventoDonacionIndependiente {
  private final String motivo;

  public EventoDonacionVencida(
      UUID donacionIndependienteId, UUID donacionOriginalId, String motivo) {
    super(donacionIndependienteId, donacionOriginalId);
    this.motivo = motivo;
  }
}
