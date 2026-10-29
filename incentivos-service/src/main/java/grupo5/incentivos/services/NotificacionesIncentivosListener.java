package grupo5.incentivos.services;

import grupo5.incentivos.infrastructure.IN8nClient;
import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacciona a los eventos de dominio del donante.
 *
 * <p>Los avisos a Notificaciones (misión cumplida, ascenso) tienen dos caminos. Con el outbox
 * activo ya quedaron registrados en la misma transacción que el guardado del donante, y un relay
 * los publica: acá no se envían de nuevo. Sin outbox (perfil en memoria, RabbitMQ deshabilitado o
 * {@code incentivos.outbox.enabled=false}) se envían directo, como antes. El aviso a n8n no pasa
 * por el outbox: sale siempre desde acá.
 */
@Component
public class NotificacionesIncentivosListener {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesIncentivosListener.class);

  private final INotificacionesClient notificacionesClient;
  private final IN8nClient n8nClient;
  private final Optional<IOutboxIncentivos> outbox;

  /** Sin outbox: los avisos se envían directo. */
  public NotificacionesIncentivosListener(
      INotificacionesClient notificacionesClient, IN8nClient n8nClient) {
    this(notificacionesClient, n8nClient, Optional.empty());
  }

  @Autowired
  public NotificacionesIncentivosListener(
      INotificacionesClient notificacionesClient,
      IN8nClient n8nClient,
      Optional<IOutboxIncentivos> outbox) {
    this.notificacionesClient = notificacionesClient;
    this.n8nClient = n8nClient;
    this.outbox = outbox;
  }

  @EventListener
  public void onMisionCompletada(MisionCompletada evento) {
    log.info(
        "Procesando MisionCompletada para donante {}: mision {}",
        evento.donanteId(),
        evento.nombreMision());

    Insignia insignia = evento.insignia();
    String recompensa = insignia != null ? insignia.nombre() : "Sin recompensa";

    if (outbox.isEmpty()) {
      notificacionesClient.notificarMisionCumplida(
          evento.idPersona(), evento.nombreMision(), recompensa);
    }

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

    if (outbox.isEmpty()) {
      notificacionesClient.notificarAscensoCategoria(
          evento.idPersona(), evento.categoriaNueva().name(), evento.categoriaAnterior().name());
    }
  }
}
