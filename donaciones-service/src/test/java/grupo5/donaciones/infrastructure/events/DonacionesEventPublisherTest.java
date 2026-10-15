package grupo5.donaciones.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.config.RabbitMQConfig;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** SPEC-03 §7.3: routing keys versionadas `.v1` y headers de trazabilidad en cada publicación. */
class DonacionesEventPublisherTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final DonacionesEventPublisher publisher = new DonacionesEventPublisher(rabbitTemplate);

  @AfterEach
  void limpiarMdc() {
    MDC.clear();
  }

  private Message publicarYCapturar(
      Consumer<DonacionesEventPublisher> publicacion, String routingKeyEsperada) {
    publicacion.accept(publisher);
    ArgumentCaptor<MessagePostProcessor> postProcessor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_DONACIONES),
            eq(routingKeyEsperada),
            isNull(),
            postProcessor.capture());
    return postProcessor
        .getValue()
        .postProcessMessage(new Message(new byte[0], new MessageProperties()));
  }

  @Test
  void cadaEvento_sePublicaConSuRoutingKeyVersionada() {
    Map<String, Consumer<DonacionesEventPublisher>> publicaciones =
        Map.of(
            "donante.registrado.v1", p -> p.publicarDonanteRegistrado(null),
            "donante.dado-de-baja.v1", p -> p.publicarDonanteDadoDeBaja(null),
            "donacion.asignada.v1", p -> p.publicarDonacionAsignada(null),
            "donacion.en-camino.v1", p -> p.publicarDonacionEnCamino(null),
            "donacion.recibida.v1", p -> p.publicarDonacionRecibida(null),
            "donacion.entrega-fallida.v1", p -> p.publicarDonacionEntregaFallida(null),
            "donacion.vencida.v1", p -> p.publicarDonacionVencida(null),
            "persona.sincronizada.v1", p -> p.publicarPersonaSincronizada(null),
            "donacion.segmentada.v1", p -> p.publicarDonacionSegmentada(null));

    publicaciones.forEach(
        (routingKey, publicacion) -> {
          Message mensaje = publicarYCapturar(publicacion, routingKey);
          assertThat(mensaje.getMessageProperties().getMessageId()).isNotBlank();
        });
  }

  @Test
  void publicar_propagaElTraceIdDelMdc() {
    MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, "trace-123");

    Message mensaje =
        publicarYCapturar(p -> p.publicarDonacionAsignada(null), "donacion.asignada.v1");

    assertThat(
            (String)
                mensaje.getMessageProperties().getHeader(FeignTraceRequestInterceptor.TRACE_HEADER))
        .isEqualTo("trace-123");
  }

  @Test
  void publicar_sinTraceEnMdc_generaUnoNuevo() {
    Message mensaje =
        publicarYCapturar(p -> p.publicarDonacionVencida(null), "donacion.vencida.v1");

    assertThat(
            (String)
                mensaje.getMessageProperties().getHeader(FeignTraceRequestInterceptor.TRACE_HEADER))
        .isNotBlank();
  }

  @Test
  void publicaciones_distintas_tienenMessageIdDistinto() {
    publisher.publicarDonacionRecibida(null);
    publisher.publicarDonacionRecibida(null);
    ArgumentCaptor<MessagePostProcessor> postProcessor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate, org.mockito.Mockito.times(2))
        .convertAndSend(any(String.class), any(String.class), isNull(), postProcessor.capture());

    var ids =
        postProcessor.getAllValues().stream()
            .map(pp -> pp.postProcessMessage(new Message(new byte[0], new MessageProperties())))
            .map(m -> m.getMessageProperties().getMessageId())
            .distinct()
            .count();
    assertThat(ids).isEqualTo(2);
  }
}
