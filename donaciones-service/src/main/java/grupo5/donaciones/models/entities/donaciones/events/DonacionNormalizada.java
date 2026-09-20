package grupo5.donaciones.models.entities.donaciones.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class DonacionNormalizada extends EventoDonacion {
  public DonacionNormalizada(UUID donacionId, UUID donanteId) {
    super(donacionId, donanteId);
  }
}
