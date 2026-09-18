package grupo5.logistica.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import grupo5.logistica.dto.eventos.EventoDonacionAsignadaV1;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

/**
 * Verifica el cableado de mensajería de logística sin depender de un broker real: el binding de la
 * cola de donación asignada y la deserialización efectiva de un payload con la forma real
 * documentada en docs/arquitectura/eventos-amqp.md. Esto es lo que puede romperse silenciosamente
 * ante un cambio de configuración de RabbitMQ (routing key, exchange o mapeo de tipos mal escritos)
 * sin que ningún test unitario del listener lo detecte, porque esos mockean el broker por completo.
 */
class RabbitMQConfigTest {

  private final RabbitMQConfig config = new RabbitMQConfig();

  @Test
  void bindingDonacionAsignada_apuntaAlExchangeColaYRoutingKeyCorrectos() {
    Binding binding =
        config.bindingLogisticaDonacionesAsignadas(
            config.queueLogisticaDonacionesAsignadas(), config.donacionesExchange());

    assertEquals(RabbitMQConfig.EXCHANGE_DONACIONES, binding.getExchange());
    assertEquals(RabbitMQConfig.ROUTING_KEY_DONACION_ASIGNADA, binding.getRoutingKey());
    assertEquals(RabbitMQConfig.QUEUE_LOGISTICA_DONACIONES_ASIGNADAS, binding.getDestination());
  }

  @Test
  void classMapper_resuelveLaRoutingKeyDonacionAsignadaAlDTOTipado() {
    DefaultClassMapper classMapper = config.classMapper();
    MessageProperties props = new MessageProperties();
    props.setHeader("__TypeId__", RabbitMQConfig.ROUTING_KEY_DONACION_ASIGNADA);

    assertEquals(EventoDonacionAsignadaV1.class, classMapper.toClass(props));
  }

  @Test
  void messageConverter_deserializaUnPayloadRealDeDonacionAsignada() {
    DefaultClassMapper classMapper = config.classMapper();
    JacksonJsonMessageConverter converter = config.messageConverter(classMapper);

    String json =
        """
        {
          "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "personaDonanteId": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
          "fecha": "2026-09-15T14:30:00Z",
          "personaBeneficiariaId": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
          "descripcion": "Caja de alimentos no perecederos (10 kg)",
          "destino": {
            "calle": "Av. Medrano",
            "altura": 951,
            "codigoPostal": "C1179AAQ",
            "localidad": "CABA",
            "provincia": "Buenos Aires",
            "pais": "Argentina"
          },
          "pesoTotalKG": 10.5,
          "volumenTotalM3": 0.25,
          "categorias": ["ALIMENTOS"],
          "cantidades": 10
        }
        """;

    MessageProperties props = new MessageProperties();
    props.setHeader("__TypeId__", RabbitMQConfig.ROUTING_KEY_DONACION_ASIGNADA);
    props.setContentType("application/json");
    Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

    Object result = converter.fromMessage(message);

    assertInstanceOf(EventoDonacionAsignadaV1.class, result);
    EventoDonacionAsignadaV1 evento = (EventoDonacionAsignadaV1) result;
    assertEquals(
        UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"), evento.donacionIndependienteId());
    assertEquals(10.5, evento.pesoTotalKG());
    assertEquals(0.25, evento.volumenTotalM3());
    assertEquals("Av. Medrano", evento.destino().calle());
    assertEquals("CABA", evento.destino().localidad());
  }
}
