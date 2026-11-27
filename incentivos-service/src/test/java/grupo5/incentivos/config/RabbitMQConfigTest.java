package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

/**
 * Solo la cola del ciclo de vida del donante tiene un solo consumidor activo; las otras y las DLQ
 * se apoyan en la idempotencia y no llevan el argumento.
 */
class RabbitMQConfigTest {

  private static final String CONSUMIDOR_UNICO = "x-single-active-consumer";

  private final RabbitMQConfig config = new RabbitMQConfig();

  @Test
  void laColaDelCicloDeVidaDelDonanteTieneUnSoloConsumidorYConservaSuDlq() {
    Queue cola = config.queueIncentivosDonanteCicloDeVida();

    assertEquals(RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA, cola.getName());
    assertEquals(Boolean.TRUE, cola.getArguments().get(CONSUMIDOR_UNICO));
    assertEquals(RabbitMQConfig.DLX_INCENTIVOS, cola.getArguments().get("x-dead-letter-exchange"));
    assertEquals(
        RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA + RabbitMQConfig.SUFIJO_DLQ,
        cola.getArguments().get("x-dead-letter-routing-key"));
  }

  @Test
  void lasOtrasColasYLasDlqNoLlevanElArgumento() {
    List<Queue> otras =
        List.of(
            config.queueIncentivosDonacionSegmentada(),
            config.queueIncentivosDonacionRecibida(),
            config.queueIncentivosPersonaSincronizada());
    otras.forEach(cola -> assertFalse(cola.getArguments().containsKey(CONSUMIDOR_UNICO)));

    List<Queue> dlqs =
        config
            .deadLetterIncentivos(new TopicExchange(RabbitMQConfig.DLX_INCENTIVOS))
            .getDeclarablesByType(Queue.class);
    assertEquals(RabbitMQConfig.COLAS_SUSCRIPTORAS.size(), dlqs.size());
    dlqs.forEach(dlq -> assertFalse(dlq.getArguments().containsKey(CONSUMIDOR_UNICO)));
  }
}
