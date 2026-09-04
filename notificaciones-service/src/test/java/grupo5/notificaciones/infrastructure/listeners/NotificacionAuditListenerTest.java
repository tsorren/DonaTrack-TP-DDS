package grupo5.notificaciones.infrastructure.listeners;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificacionAuditListenerTest {

  private NotificacionAuditListener listener;

  @BeforeEach
  void setUp() {
    listener = new NotificacionAuditListener();
  }

  @Test
  @DisplayName("onNotificacionEnviada registra la traza de auditoría sin lanzar excepción")
  void onNotificacionEnviada_registraEventoCorrectamente() {
    NotificacionEnviada event = new NotificacionEnviada(UUID.randomUUID(), LocalDateTime.now());

    assertDoesNotThrow(() -> listener.onNotificacionEnviada(event));
  }

  @Test
  @DisplayName("onNotificacionFallida registra la traza de auditoría sin lanzar excepción")
  void onNotificacionFallida_registraEventoCorrectamente() {
    NotificacionFallida event = new NotificacionFallida(UUID.randomUUID(), LocalDateTime.now());

    assertDoesNotThrow(() -> listener.onNotificacionFallida(event));
  }
}
