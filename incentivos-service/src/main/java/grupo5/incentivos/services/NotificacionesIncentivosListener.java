package grupo5.incentivos.services;

import grupo5.incentivos.infrastructure.IN8nClient;
import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacionesIncentivosListener {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesIncentivosListener.class);

  private final INotificacionesClient notificacionesClient;
  private final IN8nClient n8nClient;

  @EventListener
  public void onMisionCompletada(MisionCompletada evento) {
    log.info(
        "Procesando MisionCompletada para donante {}: mision {}",
        evento.donanteId(),
        evento.nombreMision());

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
  public void onAscensoDonante(AscensoDonante evento) {
    log.info(
        "Procesando AscensoDonante para donante {}: {} -> {}",
        evento.donanteId(),
        evento.categoriaAnterior(),
        evento.categoriaNueva());

    notificacionesClient.notificarAscensoCategoria(
        evento.idPersona(), evento.categoriaNueva().name(), evento.categoriaAnterior().name());
  }
}
