package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaSolicitadaV1;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class ProveedorLogisticaAmqpTest {

  private static final UUID ENVIO_ID = UUID.randomUUID();
  private static final DatosEntregaLogistica DATOS =
      new DatosEntregaLogistica(
          UUID.randomUUID(),
          UUID.randomUUID(),
          null,
          10.0,
          1.0,
          LocalDateTime.of(2026, 10, 7, 12, 0));

  private RabbitTemplate template;
  private ProveedorLogisticaAmqp proveedor;

  @BeforeEach
  void setUp() {
    template = mock(RabbitTemplate.class);
    proveedor = new ProveedorLogisticaAmqp("donatrack", template, 50);
  }

  /** Simula lo que hace RabbitMQ con la correlación cuando recibe el mensaje. */
  private void cuandoRabbitResponde(boolean ack, boolean devuelto) {
    doAnswer(
            invocacion -> {
              CorrelationData correlacion = invocacion.getArgument(4);
              if (devuelto) {
                correlacion.setReturned(
                    new ReturnedMessage(
                        new Message(new byte[0], new MessageProperties()),
                        312,
                        "NO_ROUTE",
                        RabbitMQConfig.EXCHANGE_DONACIONES,
                        "entrega.solicitada.donatrack.v1"));
              }
              correlacion.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "x"));
              return null;
            })
        .when(template)
        .convertAndSend(
            anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any());
  }

  @Test
  void enviar_deberiaPublicarElComandoDirigidoAlProveedor_CuandoRabbitConfirma() {
    cuandoRabbitResponde(true, false);

    assertDoesNotThrow(() -> proveedor.enviar(ENVIO_ID, DATOS, "trace-1"));

    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    ArgumentCaptor<CorrelationData> correlacion = ArgumentCaptor.forClass(CorrelationData.class);
    verify(template)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_DONACIONES),
            eq("entrega.solicitada.donatrack.v1"),
            payload.capture(),
            any(MessagePostProcessor.class),
            correlacion.capture());
    EventoEntregaSolicitadaV1 comando =
        assertInstanceOf(EventoEntregaSolicitadaV1.class, payload.getValue());
    assertEquals(DATOS.donacionIndependienteId(), comando.donacionIndependienteId());
    assertEquals(DATOS.fecha(), comando.fecha());
    assertEquals(ENVIO_ID.toString(), correlacion.getValue().getId());
  }

  @Test
  void enviar_deberiaUsarElEnvioComoMessageIdYPropagarElTraceId() {
    cuandoRabbitResponde(true, false);

    proveedor.enviar(ENVIO_ID, DATOS, "trace-1");

    ArgumentCaptor<MessagePostProcessor> envelope =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(template)
        .convertAndSend(anyString(), anyString(), any(Object.class), envelope.capture(), any());
    Message mensaje =
        envelope.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
    assertEquals(ENVIO_ID.toString(), mensaje.getMessageProperties().getMessageId());
    assertEquals(
        "trace-1",
        mensaje.getMessageProperties().getHeader(FeignTraceRequestInterceptor.TRACE_HEADER));
  }

  @Test
  void enviar_deberiaRechazar_CuandoRabbitDevuelveElMensajePorqueNoHayCola() {
    cuandoRabbitResponde(true, true);

    EnvioRechazadoException e =
        assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, "t"));
    assertEquals("donatrack", e.getProveedorId());
  }

  @Test
  void enviar_deberiaRechazar_CuandoRabbitHaceNack() {
    cuandoRabbitResponde(false, false);

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, "t"));
  }

  @Test
  void enviar_deberiaSerIncierto_CuandoNoLlegaElAcuseATiempo() {
    assertThrows(EnvioInciertoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, "t"));
  }

  @Test
  void enviar_deberiaRechazar_CuandoNoHayConexionConRabbit() {
    doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
        .when(template)
        .convertAndSend(
            anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any());

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, "t"));
  }

  @Test
  void enviar_deberiaSerIncierto_CuandoFallaElCanalDuranteElEnvio() {
    doThrow(new AmqpIOException(new java.io.IOException("canal cerrado")))
        .when(template)
        .convertAndSend(
            anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any());

    assertThrows(EnvioInciertoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, "t"));
  }
}
