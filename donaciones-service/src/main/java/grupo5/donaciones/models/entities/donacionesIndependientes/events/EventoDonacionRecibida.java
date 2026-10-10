package grupo5.donaciones.models.entities.donacionesIndependientes.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventoDonacionRecibida extends EventoDonacionIndependiente {
  private final UUID idNecesidad;
  private final String patenteCamion;

  public EventoDonacionRecibida(
      UUID donacionIndependienteId,
      UUID donacionOriginalId,
      UUID idNecesidad,
      String patenteCamion) {
    super(donacionIndependienteId, donacionOriginalId);
    this.idNecesidad = idNecesidad;
    this.patenteCamion = patenteCamion;
  }

  public UUID idNecesidad() {
    return idNecesidad;
  }

  public String patenteCamion() {
    return patenteCamion;
  }
}
