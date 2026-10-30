package grupo5.incentivos.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import grupo5.incentivos.models.outbox.EstadoOutbox;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class OutboxIncentivosRelayTest {

  private IOutboxIncentivos outbox;
  private RabbitTemplate rabbitTemplate;
  private OutboxIncentivosRelay relay;

  @BeforeEach
  void setUp() {
    outbox = mock(IOutboxIncentivos.class);
    rabbitTemplate = mock(RabbitTemplate.class);
    relay = new OutboxIncentivosRelay(outbox, rabbitTemplate, 50, 60);
  }

  private EntradaOutbox entradaDeInactividad() {
    return EventosSalientes.inactividad(UUID.randomUUID(), 30, LocalDateTime.now(), 3);
  }

  private void rabbitNoResponde() {
    doThrow(new AmqpConnectException(new RuntimeException("sin conexión")))
        .when(rabbitTemplate)
        .convertAndSend(
            anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
  }

  private void hayPendientes(EntradaOutbox... entradas) {
    when(outbox.reclamarPendientes(any(), eq(Duration.ofSeconds(60)), eq(50)))
        .thenReturn(List.of(entradas));
  }

  @Test
  void publicarPendientes_publicaConElIdDeLaEntradaComoMessageIdYLaMarcaPublicada() {
    EntradaOutbox entrada = entradaDeInactividad();
    hayPendientes(entrada);

    relay.publicarPendientes();

    ArgumentCaptor<MessagePostProcessor> postProcessor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_INCENTIVOS),
            eq(RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO),
            any(Object.class),
            postProcessor.capture());
    Message mensaje =
        postProcessor
            .getValue()
            .postProcessMessage(new Message(new byte[0], new MessageProperties()));
    assertEquals(entrada.id().toString(), mensaje.getMessageProperties().getMessageId());

    ArgumentCaptor<EntradaOutbox> guardada = ArgumentCaptor.forClass(EntradaOutbox.class);
    verify(outbox).actualizar(guardada.capture());
    assertEquals(EstadoOutbox.PUBLICADO, guardada.getValue().estado());
  }

  @Test
  void publicarPendientes_siRabbitFalla_reprogramaLaEntradaSinPerderla() {
    EntradaOutbox entrada = entradaDeInactividad();
    hayPendientes(entrada);
    rabbitNoResponde();

    relay.publicarPendientes();

    ArgumentCaptor<EntradaOutbox> guardada = ArgumentCaptor.forClass(EntradaOutbox.class);
    verify(outbox).actualizar(guardada.capture());
    assertEquals(EstadoOutbox.PENDIENTE, guardada.getValue().estado());
    assertEquals(1, guardada.getValue().intentos());
  }

  @Test
  void publicarPendientes_siSeAgotanLosIntentos_laDejaFallida() {
    EntradaOutbox ultimoIntento =
        EventosSalientes.inactividad(UUID.randomUUID(), 30, LocalDateTime.now(), 1);
    hayPendientes(ultimoIntento);
    rabbitNoResponde();

    relay.publicarPendientes();

    ArgumentCaptor<EntradaOutbox> guardada = ArgumentCaptor.forClass(EntradaOutbox.class);
    verify(outbox).actualizar(guardada.capture());
    assertEquals(EstadoOutbox.FALLIDO, guardada.getValue().estado());
  }

  @Test
  void publicarPendientes_unaEntradaConRoutingKeyDesconocida_noFrenaAlResto() {
    EntradaOutbox rota = EntradaOutbox.nueva("no.existe.v1", "{}", LocalDateTime.now(), 3);
    EntradaOutbox buena = entradaDeInactividad();
    hayPendientes(rota, buena);

    relay.publicarPendientes();

    verify(rabbitTemplate)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_INCENTIVOS),
            eq(RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO),
            any(Object.class),
            any(MessagePostProcessor.class));
    verify(outbox, times(2)).actualizar(any());
  }

  @Test
  void publicarPendientes_sinPendientes_noPublicaNiActualizaNada() {
    hayPendientes();

    relay.publicarPendientes();

    verify(rabbitTemplate, never())
        .convertAndSend(
            anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    verify(outbox, never()).actualizar(any());
  }
}
