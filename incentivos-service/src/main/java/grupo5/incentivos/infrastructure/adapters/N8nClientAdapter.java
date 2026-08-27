package grupo5.incentivos.infrastructure.adapters;

import grupo5.incentivos.infrastructure.IN8nClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class N8nClientAdapter implements IN8nClient {

  private static final Logger log = LoggerFactory.getLogger(N8nClientAdapter.class);

  private final WebClient webClient;
  private final String insigniaWebhookUrl;
  private final String rankingWebhookUrl;

  public N8nClientAdapter(
      @Value("${n8n.webhook.insignia-url}") String insigniaWebhookUrl,
      @Value("${n8n.webhook.ranking-url}") String rankingWebhookUrl) {
    this.webClient = WebClient.builder().build();
    this.insigniaWebhookUrl = insigniaWebhookUrl;
    this.rankingWebhookUrl = rankingWebhookUrl;
  }

  @Override
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

  @Override
  public void notificarRankingCalculado(String periodo, List<Map<String, Object>> top3) {

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
