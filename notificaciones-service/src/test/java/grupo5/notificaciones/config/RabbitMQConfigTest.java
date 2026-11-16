package grupo5.notificaciones.config;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
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
            .filter(k -> !k.startsWith("incentivo.") && !k.startsWith("cuenta."))
            .collect(Collectors.toSet());

    assertThat(bindings)
        .extracting(Binding::getRoutingKey)
        .doesNotContain("donacion.#", "donante.#", "persona.#")
        .containsExactlyInAnyOrderElementsOf(clavesConTipo);
  }

  @Test
  void colaDeCuentas_soloSeBindeaAClavesDeCuenta() {
    Binding binding =
        config.bindingNotificacionesAuth(
            new Queue(RabbitMQConfig.QUEUE_NOTIFICACIONES_AUTH),
            new TopicExchange(RabbitMQConfig.EXCHANGE_AUTH));

    assertThat(binding.getExchange()).isEqualTo("auth.exchange");
    assertThat(binding.getDestination()).isEqualTo("notificaciones.auth");
    assertThat(binding.getRoutingKey()).isEqualTo("cuenta.#");
  }

  @Test
  void colaDeCuentas_vaALaColaDeMuertosSiFalla() {
    Queue cola = config.queueNotificacionesAuth();
    Binding muertos =
        config.bindingNotificacionesAuthDlq(
            config.queueNotificacionesAuthDlq(), config.notificacionesDlx());

    assertThat(cola.isDurable()).isTrue();
    assertThat(cola.getArguments())
        .containsEntry("x-dead-letter-exchange", RabbitMQConfig.DLX_NOTIFICACIONES)
        .containsEntry("x-dead-letter-routing-key", "notificaciones.auth.dlq");
    assertThat(muertos.getDestination()).isEqualTo("notificaciones.auth.dlq");
    assertThat(muertos.getRoutingKey()).isEqualTo("notificaciones.auth.dlq");
  }

  @Test
  void elEventoDeCuentaTieneTipoMapeadoConLaClaveDelContrato() {
    @SuppressWarnings("unchecked")
    Map<String, Class<?>> mapeo =
        (Map<String, Class<?>>)
            org.springframework.test.util.ReflectionTestUtils.getField(
                config.classMapper(), "idClassMapping");

    assertThat(mapeo)
        .containsEntry("cuenta.enlace-solicitado.v1", EventoCuentaEnlaceSolicitadoV1.class);
    assertThat(RabbitMQConfig.ROUTING_KEY_CUENTA_ENLACE_SOLICITADO)
        .isEqualTo("cuenta.enlace-solicitado.v1");
  }
}
