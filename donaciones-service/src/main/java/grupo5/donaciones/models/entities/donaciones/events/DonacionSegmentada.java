package grupo5.donaciones.models.entities.donaciones.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class DonacionSegmentada extends EventoDonacion {
  public DonacionSegmentada(UUID donacionId, UUID donanteId) {
    super(donacionId, donanteId);
  }
}
