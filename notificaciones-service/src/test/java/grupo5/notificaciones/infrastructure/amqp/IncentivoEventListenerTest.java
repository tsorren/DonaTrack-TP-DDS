package grupo5.notificaciones.infrastructure.amqp;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.notificaciones.dto.input.EventoIncentivoDonanteInactivoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncentivoEventListenerTest {

  @Mock private NotificacionService notificacionService;

  private IncentivoEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new IncentivoEventListener(notificacionService);
  }

  @Test
  void onMisionCumplida_deberiaDelegarEnServicio() {
    EventoIncentivoMisionCumplidaV1 evento =
        new EventoIncentivoMisionCumplidaV1(
            UUID.randomUUID(), LocalDateTime.now(), "Racha 5 donaciones", "Medalla Plata");
    String messageId = UUID.randomUUID().toString();

    listener.onMisionCumplida(evento, messageId);

    verify(notificacionService, times(1)).procesar(evento, messageId);
  }

  @Test
  void onSubioCategoria_deberiaDelegarEnServicio() {
    EventoIncentivoSubioCategoriaV1 evento =
        new EventoIncentivoSubioCategoriaV1(
            UUID.randomUUID(), LocalDateTime.now(), "Platino", "Oro");
    String messageId = UUID.randomUUID().toString();

    listener.onSubioCategoria(evento, messageId);

    verify(notificacionService, times(1)).procesar(evento, messageId);
  }

  @Test
  void onDonanteInactivo_deberiaDelegarEnServicio() {
    EventoIncentivoDonanteInactivoV1 evento =
        new EventoIncentivoDonanteInactivoV1(UUID.randomUUID(), LocalDateTime.now(), 30);
    String messageId = UUID.randomUUID().toString();

    listener.onDonanteInactivo(evento, messageId);

    verify(notificacionService, times(1)).procesar(evento, messageId);
  }
}
