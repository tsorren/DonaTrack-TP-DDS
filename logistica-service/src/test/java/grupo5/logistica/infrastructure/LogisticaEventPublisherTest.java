package grupo5.logistica.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import grupo5.logistica.config.RabbitMQConfig;
import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class LogisticaEventPublisherTest {

  private RabbitTemplate rabbitTemplate;

  @BeforeEach
  void setUp() {
    rabbitTemplate = mock(RabbitTemplate.class);
  }

  private MessageProperties propiedadesQueSeAgregan(String routingKey) {
    ArgumentCaptor<MessagePostProcessor> captor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE), eq(routingKey), any(Object.class), captor.capture());
    Message mensaje = new Message(new byte[0], new MessageProperties());
    return captor.getValue().postProcessMessage(mensaje).getMessageProperties();
  }

  @Test
  void cadaEventoLlevaElIdYElTokenDeEstaInstancia() {
    LogisticaEventPublisher publisher =
        new LogisticaEventPublisher(rabbitTemplate, "externo", "token-sintetico");
    UUID donacionId = UUID.randomUUID();

    publisher.publicarRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now()));

    MessageProperties propiedades =
        propiedadesQueSeAgregan(RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA);
    assertEquals("externo", propiedades.getHeader(RabbitMQConfig.HEADER_PROVEEDOR_ID));
    assertEquals("token-sintetico", propiedades.getHeader(RabbitMQConfig.HEADER_PROVEEDOR_TOKEN));
  }

  @Test
  void losCuatroEventosSeFirmanConLaMismaIdentidad() {
    LogisticaEventPublisher publisher =
        new LogisticaEventPublisher(rabbitTemplate, "donatrack", "token-sintetico");
    UUID id = UUID.randomUUID();

    publisher.publicarRutaIniciada(
        new EventoRutaIniciada(id, id, "AB123CD", List.of(id), LocalDateTime.now(), "http://mapa"));
    publisher.publicarEntregaExitosa(
        new EventoEntregaExitosa(id, id, id, "AB123CD", LocalDateTime.now()));
    publisher.publicarEntregaFallida(
        new EventoEntregaFallida(id, id, "sin acceso", LocalDateTime.now(), false));

    for (String clave :
        new String[] {
          RabbitMQConfig.ROUTING_KEY_RUTA_INICIADA,
          RabbitMQConfig.ROUTING_KEY_ENTREGA_EXITOSA,
          RabbitMQConfig.ROUTING_KEY_ENTREGA_FALLIDA
        }) {
      MessageProperties propiedades = propiedadesQueSeAgregan(clave);
      assertEquals("donatrack", propiedades.getHeader(RabbitMQConfig.HEADER_PROVEEDOR_ID), clave);
      assertEquals(
          "token-sintetico", propiedades.getHeader(RabbitMQConfig.HEADER_PROVEEDOR_TOKEN), clave);
    }
  }

  @Test
  void sinTokenConfigurado_seMandaSoloElIdYNuncaUnTokenVacio() {
    LogisticaEventPublisher publisher =
        new LogisticaEventPublisher(rabbitTemplate, "donatrack", "");

    publisher.publicarRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now()));

    MessageProperties propiedades =
        propiedadesQueSeAgregan(RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA);
    assertEquals("donatrack", propiedades.getHeader(RabbitMQConfig.HEADER_PROVEEDOR_ID));
    assertFalse(propiedades.getHeaders().containsKey(RabbitMQConfig.HEADER_PROVEEDOR_TOKEN));
  }
}
