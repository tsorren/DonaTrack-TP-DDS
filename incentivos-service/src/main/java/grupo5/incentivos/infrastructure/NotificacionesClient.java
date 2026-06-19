package grupo5.incentivos.infrastructure;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificacionesClient {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesClient.class);

  private final NotificacionesFeignClient feignClient;

  public NotificacionesClient(NotificacionesFeignClient feignClient) {
    this.feignClient = feignClient;
  }

  @Async
  public void notificarMisionCumplida(
      UUID personaId, UUID donanteId, String nombreMision, String recompensa) {
    enviar(
        new EventoMisionCumplidaRequest(
            personaId, LocalDateTime.now(ZoneId.systemDefault()), nombreMision, recompensa),
        "MISION_CUMPLIDA",
        donanteId);
  }

  @Async
  public void notificarAscensoCategoria(
      UUID personaId, UUID donanteId, String categoriaNueva, String categoriaVieja) {
    enviar(
        new EventoSubioCategoriaRequest(
            personaId, LocalDateTime.now(ZoneId.systemDefault()), categoriaNueva, categoriaVieja),
        "SUBIO_CATEGORIA",
        donanteId);
  }

  @Async
  public void notificarInactividad(UUID personaId, UUID donanteId, int diasInactivo) {
    enviar(
        new EventoDonanteInactivoRequest(
            personaId, LocalDateTime.now(ZoneId.systemDefault()), diasInactivo),
        "DONANTE_INACTIVO",
        donanteId);
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
