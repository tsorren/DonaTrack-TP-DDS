package grupo5.incentivos.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class IncentivosEventosPublisherTest {

  @Test
  void cadaPublicacion_llevaUnMessageIdPropio_paraElInboxDeNotificaciones() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    IncentivosEventosPublisher publisher = new IncentivosEventosPublisher(rabbitTemplate);
    UUID persona = UUID.randomUUID();

    publisher.notificarMisionCumplida(persona, "Primera donación", "10 puntos");
    publisher.notificarMisionCumplida(persona, "Tercera donación", "30 puntos");

    ArgumentCaptor<MessagePostProcessor> postProcessor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate, times(2))
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_INCENTIVOS),
            eq(RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA),
            any(Object.class),
            postProcessor.capture());
    long idsDistintos =
        postProcessor.getAllValues().stream()
            .map(pp -> pp.postProcessMessage(new Message(new byte[0], new MessageProperties())))
            .map(m -> m.getMessageProperties().getMessageId())
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .count();
    assertThat(idsDistintos).isEqualTo(2);
  }

  @Test
  void notificarInactividad_conOutbox_registraElAvisoYNoPublicaDirecto() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    IOutboxIncentivos outbox = mock(IOutboxIncentivos.class);
    IncentivosEventosPublisher publisher =
        new IncentivosEventosPublisher(rabbitTemplate, Optional.of(outbox));
    UUID persona = UUID.randomUUID();

    publisher.notificarInactividad(persona, 45);

    verify(outbox).registrarInactividad(persona, 45);
    verifyNoInteractions(rabbitTemplate);
  }

  @Test
  void notificarInactividad_sinOutbox_publicaDirecto() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    IncentivosEventosPublisher publisher = new IncentivosEventosPublisher(rabbitTemplate);

    publisher.notificarInactividad(UUID.randomUUID(), 45);

    verify(rabbitTemplate)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_INCENTIVOS),
            eq(RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO),
            any(Object.class),
            any(MessagePostProcessor.class));
  }
}
