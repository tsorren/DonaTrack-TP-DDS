package grupo5.logistica.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoEntregaSolicitadaV1;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

/**
 * Verifica el cableado de mensajería de logística sin depender de un broker real: el binding de la
 * cola de pedidos de entrega de esta instancia y la deserialización efectiva de un comando con la
 * forma real del contrato. Esto es lo que puede romperse silenciosamente ante un cambio de
 * configuración de RabbitMQ (routing key, exchange o mapeo de tipos mal escritos) sin que ningún
 * test unitario del listener lo detecte, porque esos mockean el broker por completo.
 */
class RabbitMQConfigTest {

  private final RabbitMQConfig config = new RabbitMQConfig("donatrack");

  @Test
  void bindingEntregasSolicitadas_apuntaAlExchangeDeDonacionesConLaClaveExactaDeLaInstancia() {
    Binding binding =
        config.bindingEntregasSolicitadas(
            config.queueEntregasSolicitadas(), config.donacionesExchange());

    assertEquals(RabbitMQConfig.EXCHANGE_DONACIONES, binding.getExchange());
    assertEquals("entrega.solicitada.donatrack.v1", binding.getRoutingKey());
    assertEquals("logistica.donatrack.entregas.solicitadas", binding.getDestination());
  }

  @Test
  void cadaInstanciaTieneSuPropiaColaYSuPropiaClave() {
    RabbitMQConfig otra = new RabbitMQConfig("externo");

    assertNotEquals(config.nombreColaEntregasSolicitadas(), otra.nombreColaEntregasSolicitadas());
    assertNotEquals(config.routingKeyEntregaSolicitada(), otra.routingKeyEntregaSolicitada());
    assertEquals("entrega.solicitada.externo.v1", otra.routingKeyEntregaSolicitada());
  }

  @Test
  void classMapper_resuelveElAliasFijoDelComandoAlDTOTipado() {
    DefaultClassMapper classMapper = config.classMapper();
    MessageProperties props = new MessageProperties();
    props.setHeader("__TypeId__", RabbitMQConfig.TYPE_ID_ENTREGA_SOLICITADA);

    assertEquals(EventoEntregaSolicitadaV1.class, classMapper.toClass(props));
  }

  @Test
  void messageConverter_deserializaUnComandoRealDeEntregaSolicitada() {
    DefaultClassMapper classMapper = config.classMapper();
    JacksonJsonMessageConverter converter = config.messageConverter(classMapper);

    String json =
        """
        {
          "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "personaBeneficiariaId": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
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
          "fecha": "2026-09-15T14:30:00Z"
        }
        """;

    MessageProperties props = new MessageProperties();
    props.setHeader("__TypeId__", RabbitMQConfig.TYPE_ID_ENTREGA_SOLICITADA);
    props.setContentType("application/json");
    Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

    Object result = converter.fromMessage(message);

    assertInstanceOf(EventoEntregaSolicitadaV1.class, result);
    EventoEntregaSolicitadaV1 comando = (EventoEntregaSolicitadaV1) result;
    assertEquals(
        UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"), comando.donacionIndependienteId());
    assertEquals(10.5, comando.pesoTotalKG());
    assertEquals(0.25, comando.volumenTotalM3());
    assertEquals("Av. Medrano", comando.destino().calle());
    assertEquals("CABA", comando.destino().localidad());
  }

  @Test
  void classMapper_publicaLosEventosDeVueltaConElAliasDeLaRoutingKeyYNoConElNombreDeLaClase() {
    DefaultClassMapper classMapper = config.classMapper();
    // El mapeo clase -> alias se arma al inicializar el bean; Spring lo hace al crearlo.
    classMapper.afterPropertiesSet();
    Map<Class<?>, String> esperados =
        Map.of(
            EventoRutaAsignada.class, "ruta.asignada",
            EventoRutaIniciada.class, "ruta.iniciada",
            EventoEntregaExitosa.class, "entrega.exitosa",
            EventoEntregaFallida.class, "entrega.fallida");

    esperados.forEach(
        (clase, alias) -> {
          MessageProperties props = new MessageProperties();
          classMapper.fromClass(clase, props);
          assertEquals(alias, props.getHeaders().get("__TypeId__"), clase.getSimpleName());
        });
  }
}
