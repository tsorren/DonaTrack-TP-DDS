package grupo5.incentivos.services;

import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionesIncentivosListener {

  private final NotificacionesClient notificacionesClient;
  private final N8nClient n8nClient;

  public NotificacionesIncentivosListener(
      NotificacionesClient notificacionesClient, N8nClient n8nClient) {
    this.notificacionesClient = notificacionesClient;
    this.n8nClient = n8nClient;
  }

  @EventListener
  public void onMisionCompletada(MisionCompletada evento) {
    Insignia insignia = evento.insignia();
    String recompensa = insignia != null ? insignia.nombre() : "Sin recompensa";

    notificacionesClient.notificarMisionCumplida(
        evento.idPersona(), evento.nombreMision(), recompensa);

    if (insignia != null) {
      n8nClient.publicarInsigniaGanada(
          evento.donanteId(),
          "Donante " + evento.nombreDonante(),
          insignia.nombre(),
          insignia.descripcion());
    }
  }

  @EventListener
  public void onDonanteAscendio(AscensoDonante evento) {
    notificacionesClient.notificarAscensoCategoria(
        evento.idPersona(), evento.categoriaNueva().name(), evento.categoriaAnterior().name());
  }
}
