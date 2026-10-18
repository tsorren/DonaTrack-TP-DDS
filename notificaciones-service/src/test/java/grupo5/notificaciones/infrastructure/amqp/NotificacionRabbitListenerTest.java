package grupo5.notificaciones.infrastructure.amqp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.notificaciones.dto.input.EventoDonanteRegistradoDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionRabbitListenerTest {

  @Mock private NotificacionService notificacionService;

  private NotificacionRabbitListener listener;

  @BeforeEach
  void setUp() {
    listener = new NotificacionRabbitListener(notificacionService);
  }

  @Test
  void deberiaDelegarEnServicioAlRecibirEvento() {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "user123");

    listener.onEventoNotificable(dto);

    verify(notificacionService, times(1)).procesar(dto);
  }

  @Test
  void deberiaCapturarValidationExceptionSinRelanzar() {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "user123");
    doThrow(new ValidationException("Persona no encontrada"))
        .when(notificacionService)
        .procesar(dto);

    assertDoesNotThrow(() -> listener.onEventoNotificable(dto));
  }

  @Test
  void deberiaCapturarIllegalArgumentExceptionSinRelanzar() {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "user123");
    doThrow(new IllegalArgumentException("Dato invalido")).when(notificacionService).procesar(dto);

    assertDoesNotThrow(() -> listener.onEventoNotificable(dto));
  }

  @Test
  void deberiaCapturarNoSuchElementExceptionSinRelanzar() {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "user123");
    doThrow(new NoSuchElementException("No existe elemento"))
        .when(notificacionService)
        .procesar(dto);

    assertDoesNotThrow(() -> listener.onEventoNotificable(dto));
  }

  @Test
  void deberiaPropagarExcepcionesTransitorias() {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "user123");
    doThrow(new RuntimeException("Error de conexion JDBC")).when(notificacionService).procesar(dto);

    assertThrows(RuntimeException.class, () -> listener.onEventoNotificable(dto));
  }
}
