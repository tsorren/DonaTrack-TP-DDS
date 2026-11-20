package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

/** Las seis colas del ciclo de vida de una entrega se declaran con un solo consumidor activo. */
class RabbitMQConfigTest {

  private final RabbitMQConfig config = new RabbitMQConfig();

  @Test
  void lasSeisColasDelCicloDeVidaDeUnaEntregaTienenUnSoloConsumidorActivo() {
    List<Queue> colas =
        List.of(
            config.queueRutaAsignada(),
            config.queueRutaIniciada(),
            config.queueEntregaEnPuerta(),
            config.queueEntregaExitosa(),
            config.queueEntregaFallida(),
            config.queueEntregaCancelada());

    assertEquals(
        List.of(
            RabbitMQConfig.QUEUE_RUTA_ASIGNADA,
            RabbitMQConfig.QUEUE_RUTA_INICIADA,
            RabbitMQConfig.QUEUE_ENTREGA_EN_PUERTA,
            RabbitMQConfig.QUEUE_ENTREGA_EXITOSA,
            RabbitMQConfig.QUEUE_ENTREGA_FALLIDA,
            RabbitMQConfig.QUEUE_ENTREGA_CANCELADA),
        colas.stream().map(Queue::getName).toList());
    for (Queue cola : colas) {
      assertTrue(cola.isDurable(), cola.getName());
      assertEquals(
          Boolean.TRUE, cola.getArguments().get("x-single-active-consumer"), cola.getName());
    }
  }
}
