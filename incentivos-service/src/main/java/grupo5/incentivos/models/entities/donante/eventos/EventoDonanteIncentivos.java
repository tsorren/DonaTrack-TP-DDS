package grupo5.incentivos.models.entities.donante.eventos;

import grupo5.common.events.EventoDeDominio;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class EventoDonanteIncentivos extends EventoDeDominio {
  private final UUID donanteId;
  private final UUID idPersona;

  protected EventoDonanteIncentivos(UUID donanteId, UUID idPersona) {
    super();
    this.donanteId = donanteId;
    this.idPersona = idPersona;
  }

  public UUID donanteId() {
    return donanteId;
  }

  public UUID idPersona() {
    return idPersona;
  }
}
