package grupo5.incentivos.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NotificacionesClient {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesClient.class);

  private final WebClient webClient;

  public NotificacionesClient(@Value("${notificaciones.service.url}") String baseUrl) {
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
  }

  public void notificarMisionCumplida(Long donanteId, String nombreMision, String recompensa) {
    enviarNotificacion(
        donanteId,
        "MISION_CUMPLIDA",
        "Completaste la mision " + nombreMision + ". Recompensa: " + recompensa);
  }

  public void notificarAscensoCategoria(Long donanteId, String nuevaCategoria) {
    enviarNotificacion(
        donanteId, "ASCENSO_CATEGORIA", "¡Subiste a la categoría " + nuevaCategoria + "!");
  }

  private void enviarNotificacion(Long donanteId, String tipoEvento, String mensaje) {
    try {
      webClient
          .post()
          .uri("/api/notificaciones")
          .bodyValue(new NotificacionRequest(donanteId, tipoEvento, mensaje))
          .retrieve()
          .toBodilessEntity()
          .subscribe(
              response ->
                  log.info("Notificacion enviada para donante {}: {}", donanteId, tipoEvento),
              error ->
                  log.warn("Error al notificar donante {}: {}", donanteId, error.getMessage()));
    } catch (Exception e) {
      log.warn("No se pudo contactar al servicio de notificaciones: {}", e.getMessage());
    }
  }

  record NotificacionRequest(Long donanteId, String tipoEvento, String mensaje) {}
}
