package grupo5.donaciones.models.entities.donacionesIndependientes.events;

import grupo5.common.events.EventoDeDominio;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class EventoDonacionIndependiente extends EventoDeDominio {
  private final UUID donacionIndependienteId;
  private final UUID donacionOriginalId;

  protected EventoDonacionIndependiente(UUID donacionIndependienteId, UUID donacionOriginalId) {
    super();
    this.donacionIndependienteId = donacionIndependienteId;
    this.donacionOriginalId = donacionOriginalId;
  }

  public UUID donacionIndependienteId() {
    return donacionIndependienteId;
  }

  public UUID donacionOriginalId() {
    return donacionOriginalId;
  }
}
