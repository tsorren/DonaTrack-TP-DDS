package grupo5.donaciones.models.entities.donacionesIndependientes.events;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventoRutaIniciada extends EventoDonacionIndependiente {
  private final UUID idNecesidad;
  private final String urlMapa;

  public EventoRutaIniciada(
      UUID donacionIndependienteId, UUID donacionOriginalId, UUID idNecesidad, String urlMapa) {
    super(donacionIndependienteId, donacionOriginalId);
    this.idNecesidad = idNecesidad;
    this.urlMapa = urlMapa;
  }

  public UUID idNecesidad() {
    return idNecesidad;
  }

  public String urlMapa() {
    return urlMapa;
  }
}
