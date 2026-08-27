package grupo5.incentivos.infrastructure.adapters;

import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificacionesClientAdapter implements INotificacionesClient {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesClientAdapter.class);

  private final NotificacionesFeignClient feignClient;

  public NotificacionesClientAdapter(NotificacionesFeignClient feignClient) {
    this.feignClient = feignClient;
  }

  @Override
  @Async
  public void notificarMisionCumplida(UUID idPersona, String nombreMision, String recompensa) {
    enviar(
        new EventoMisionCumplidaRequest(
            idPersona, LocalDateTime.now(ZoneId.systemDefault()), nombreMision, recompensa),
        "MISION_CUMPLIDA",
        idPersona);
  }

  @Override
  @Async
  public void notificarAscensoCategoria(
      UUID idPersona, String categoriaNueva, String categoriaVieja) {
    enviar(
        new EventoSubioCategoriaRequest(
            idPersona, LocalDateTime.now(ZoneId.systemDefault()), categoriaNueva, categoriaVieja),
        "SUBIO_CATEGORIA",
        idPersona);
  }

  @Override
  @Async
  public void notificarInactividad(UUID idPersona, int diasInactivo) {
    enviar(
        new EventoDonanteInactivoRequest(
            idPersona, LocalDateTime.now(ZoneId.systemDefault()), diasInactivo),
        "DONANTE_INACTIVO",
        idPersona);
  }

  private void enviar(Object evento, String tipoEvento, UUID donanteId) {
    try {
      feignClient.procesarEvento(evento);
      log.info("Notificacion enviada para donante {}: {}", donanteId, tipoEvento);
    } catch (Exception e) {
      log.warn(
          "No se pudo contactar al servicio de notificaciones (donante {}, evento {}): {}",
          donanteId,
          tipoEvento,
          e.getMessage());
    }
  }

  record EventoMisionCumplidaRequest(
      UUID idPersonaDonante,
      LocalDateTime fecha,
      String nombreMision,
      String recompensa,
      String tipo) {
    EventoMisionCumplidaRequest(
        UUID idPersonaDonante, LocalDateTime fecha, String nombreMision, String recompensa) {
      this(idPersonaDonante, fecha, nombreMision, recompensa, "MISION_CUMPLIDA");
    }
  }

  record EventoSubioCategoriaRequest(
      UUID idPersonaDonante,
      LocalDateTime fecha,
      String categoriaNueva,
      String categoriaVieja,
      String tipo) {
    EventoSubioCategoriaRequest(
        UUID idPersonaDonante, LocalDateTime fecha, String categoriaNueva, String categoriaVieja) {
      this(idPersonaDonante, fecha, categoriaNueva, categoriaVieja, "SUBIO_CATEGORIA");
    }
  }

  record EventoDonanteInactivoRequest(
      UUID idPersonaDonante, LocalDateTime fecha, Integer diasInactivo, String tipo) {
    EventoDonanteInactivoRequest(UUID idPersonaDonante, LocalDateTime fecha, Integer diasInactivo) {
      this(idPersonaDonante, fecha, diasInactivo, "DONANTE_INACTIVO");
    }
  }
}
