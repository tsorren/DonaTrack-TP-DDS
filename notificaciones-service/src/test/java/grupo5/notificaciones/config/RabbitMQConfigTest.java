package grupo5.notificaciones.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class RabbitMQConfigTest {

  private final RabbitMQConfig config = new RabbitMQConfig();

  @Test
  void colaDeDonaciones_soloSeBindeaAClavesConTipoMapeado() {
    var bindings =
        config
            .bindingsNotificacionesDonaciones(
                new Queue(RabbitMQConfig.QUEUE_NOTIFICACIONES_DONACIONES),
                new TopicExchange(RabbitMQConfig.EXCHANGE_DONACIONES))
            .getDeclarablesByType(Binding.class);
    @SuppressWarnings("unchecked")
    Map<String, Class<?>> mapeo =
        (Map<String, Class<?>>)
            org.springframework.test.util.ReflectionTestUtils.getField(
                config.classMapper(), "idClassMapping");
    var clavesConTipo =
        mapeo.keySet().stream()
            .filter(k -> !k.startsWith("incentivo."))
            .collect(Collectors.toSet());

    assertThat(bindings)
        .extracting(Binding::getRoutingKey)
        .doesNotContain("donacion.#", "donante.#", "persona.#")
        .containsExactlyInAnyOrderElementsOf(clavesConTipo);
  }
}
