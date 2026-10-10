package grupo5.donaciones.models.entities.donaciones.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class DonacionCargada extends EventoDonacion {
  public DonacionCargada(UUID donacionId, UUID donanteId) {
    super(donacionId, donanteId);
  }
}
