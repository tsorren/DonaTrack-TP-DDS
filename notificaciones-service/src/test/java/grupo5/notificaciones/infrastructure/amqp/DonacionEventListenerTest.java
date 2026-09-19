package grupo5.notificaciones.infrastructure.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.notificaciones.dto.input.DestinoEventoDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonacionEventListenerTest {

  @Mock private NotificacionService notificacionService;

  private DonacionEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new DonacionEventListener(notificacionService);
  }

  private EventoDonacionAsignadaV1 crearEventoValido() {
    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Av. Corrientes", 1234, 3, "B", "C1043", "San Nicolás", "CABA", "Argentina");
    return new EventoDonacionAsignadaV1(
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDateTime.of(2026, 9, 17, 12, 0),
        UUID.randomUUID(),
        "Ropa de invierno",
        destino,
        15.5,
        0.3);
  }

  @Test
  void onDonacionAsignada_conMessageId_deberiaDelegarEnServicio() {
    EventoDonacionAsignadaV1 evento = crearEventoValido();
    String messageId = UUID.randomUUID().toString();

    listener.onDonacionAsignada(evento, messageId);

    verify(notificacionService, times(1)).procesar(evento, messageId);
  }

  @Test
  void onDonacionAsignada_sinMessageId_deberiaDelegarConMessageIdNulo() {
    EventoDonacionAsignadaV1 evento = crearEventoValido();

    listener.onDonacionAsignada(evento, null);

    verify(notificacionService, times(1)).procesar(evento, null);
  }

  @Test
  void onDonacionAsignada_cuandoServicioFalla_deberiaPropagarExcepcionParaDLQ() {
    EventoDonacionAsignadaV1 evento = crearEventoValido();
    String messageId = UUID.randomUUID().toString();
    RuntimeException errorEsperado = new RuntimeException("Error inesperado en procesamiento");

    doThrow(errorEsperado).when(notificacionService).procesar(evento, messageId);

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> listener.onDonacionAsignada(evento, messageId));

    assertEquals("Error inesperado en procesamiento", ex.getMessage());
    verify(notificacionService, times(1)).procesar(evento, messageId);
  }
}
