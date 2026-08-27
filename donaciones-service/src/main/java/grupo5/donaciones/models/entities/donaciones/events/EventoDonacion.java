package grupo5.donaciones.models.entities.donaciones.events;

import grupo5.common.events.EventoDeDominio;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class EventoDonacion extends EventoDeDominio {
  private final UUID donacionId;
  private final UUID donanteId;

  protected EventoDonacion(UUID donacionId, UUID donanteId) {
    super();
    this.donacionId = donacionId;
    this.donanteId = donanteId;
  }

  public UUID donacionId() {
    return donacionId;
  }

  public UUID donanteId() {
    return donanteId;
  }
}
