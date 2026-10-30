package grupo5.incentivos.infrastructure.outbox;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import grupo5.incentivos.models.outbox.EstadoOutbox;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publica en RabbitMQ los avisos pendientes del outbox. Mismo papel que {@code
 * LogisticaOutboxRelay} de donaciones-service.
 *
 * <p>El id de cada entrada viaja como {@code messageId}: es estable entre reintentos y entre
 * réplicas, así que si una entrada se publica dos veces (el relay se cae antes de marcarla),
 * Notificaciones descarta la repetición con su inbox. Las entradas se reservan con un arrendamiento
 * ({@link IOutboxIncentivos#reclamarPendientes}), así que dos réplicas no publican las mismas.
 */
@Component
@Profile("postgres")
@ConditionalOnProperty(
    name = {"incentivos.rabbitmq.enabled", "incentivos.outbox.enabled"},
    havingValue = "true")
public class OutboxIncentivosRelay {

  private static final Logger log = LoggerFactory.getLogger(OutboxIncentivosRelay.class);

  private final IOutboxIncentivos outbox;
  private final RabbitTemplate rabbitTemplate;
  private final int lote;
  private final Duration arrendamiento;

  public OutboxIncentivosRelay(
      IOutboxIncentivos outbox,
      RabbitTemplate rabbitTemplate,
      @Value("${incentivos.outbox.lote:50}") int lote,
      @Value("${incentivos.outbox.arrendamiento-segundos:60}") long arrendamientoSegundos) {
    this.outbox = outbox;
    this.rabbitTemplate = rabbitTemplate;
    this.lote = lote;
    this.arrendamiento = Duration.ofSeconds(arrendamientoSegundos);
  }

  @Scheduled(fixedDelayString = "${incentivos.outbox.intervalo-ms:5000}")
  public void publicarPendientes() {
    List<EntradaOutbox> pendientes =
        outbox.reclamarPendientes(LocalDateTime.now(ZoneId.systemDefault()), arrendamiento, lote);
    pendientes.forEach(this::publicar);
  }

  private void publicar(EntradaOutbox entrada) {
    EntradaOutbox resultado;
    try {
      Object evento = EventosSalientes.aEvento(entrada);
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_INCENTIVOS,
          entrada.routingKey(),
          evento,
          mensaje -> {
            mensaje.getMessageProperties().setMessageId(entrada.id().toString());
            return mensaje;
          });
      resultado = entrada.publicada();
      log.info("[OUTBOX-INCENTIVOS] Aviso {} publicado ({})", entrada.id(), entrada.routingKey());
    } catch (RuntimeException e) {
      resultado = entrada.fallida(LocalDateTime.now(ZoneId.systemDefault()));
      if (resultado.estado() == EstadoOutbox.FALLIDO) {
        log.error(
            "[OUTBOX-INCENTIVOS] Aviso {} ({}) FALLIDO tras {} intentos; requiere revisión: {}",
            entrada.id(),
            entrada.routingKey(),
            resultado.intentos(),
            e.getMessage(),
            e);
      } else {
        log.warn(
            "[OUTBOX-INCENTIVOS] No se pudo publicar el aviso {} (intento {}/{}): {}",
            entrada.id(),
            resultado.intentos(),
            resultado.maxIntentos(),
            e.getMessage());
      }
    }
    try {
      outbox.actualizar(resultado);
    } catch (RuntimeException e) {
      // La entrada queda reservada y vuelve a estar disponible al vencer el arrendamiento; si ya
      // se había publicado, Notificaciones descarta la repetición por messageId.
      log.error(
          "[OUTBOX-INCENTIVOS] No se pudo actualizar el aviso {}: {}",
          entrada.id(),
          e.getMessage());
    }
  }
}
