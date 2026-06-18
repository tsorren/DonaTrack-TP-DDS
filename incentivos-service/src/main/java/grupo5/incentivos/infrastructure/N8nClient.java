package grupo5.incentivos.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Component
public class N8nClient {

  private static final Logger log = LoggerFactory.getLogger(N8nClient.class);

  private final WebClient webClient;
  private final String insigniaWebhookUrl;
  private final String rankingWebhookUrl;

  public N8nClient(
      @Value("${n8n.webhook.insignia-url}") String insigniaWebhookUrl,
      @Value("${n8n.webhook.ranking-url}") String rankingWebhookUrl) {
    this.webClient = WebClient.builder().build();
    this.insigniaWebhookUrl = insigniaWebhookUrl;
    this.rankingWebhookUrl = rankingWebhookUrl;
  }

  /**
   * Dispara el flujo de n8n cuando un donante gana una insignia. n8n se encarga de generar la
   * imagen y "publicar" en red social (mockeado).
   */
  public void publicarInsigniaGanada(
      UUID donanteId, String nombreDonante, String nombreInsignia, String descripcionInsignia) {

    Map<String, Object> payload =
        Map.of(
            "donanteId",
            donanteId,
            "user",
            nombreDonante != null ? nombreDonante : "Donante #" + donanteId,
            "badge",
            nombreInsignia,
            "description",
            descripcionInsignia != null ? descripcionInsignia : "Insignia obtenida en DonaTrack");

    log.info("Notificando a n8n: donante {} ganó insignia '{}'", donanteId, nombreInsignia);

    webClient
        .post()
        .uri(insigniaWebhookUrl)
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .subscribe(
            response -> log.info("n8n aceptó el evento de insignia para donante {}", donanteId),
            error ->
                log.warn(
                    "No se pudo contactar a n8n para la insignia del donante {}: {}",
                    donanteId,
                    error.getMessage()));
  }

  /**
   * Notifica a n8n que el ranking mensual fue calculado, para que publique los destacados (mock de
   * red social).
   */
  public void notificarRankingCalculado(String periodo, java.util.List<Map<String, Object>> top3) {

    Map<String, Object> payload =
        Map.of(
            "periodo", periodo,
            "top3", top3);

    log.info("Notificando a n8n el ranking mensual para {}", periodo);

    webClient
        .post()
        .uri(rankingWebhookUrl)
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .subscribe(
            response -> log.info("n8n aceptó el evento de ranking mensual para {}", periodo),
            error ->
                log.warn(
                    "No se pudo contactar a n8n para el ranking de {}: {}",
                    periodo,
                    error.getMessage()));
  }
}
