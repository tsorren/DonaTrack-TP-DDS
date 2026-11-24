package grupo5.notificaciones.infrastructure.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificacionMetricasListenerTest {

  private final UUID id = UUID.randomUUID();
  private final LocalDateTime fecha = LocalDateTime.now();
  private SimpleMeterRegistry registro;
  private NotificacionMetricasListener listener;

  @BeforeEach
  void setUp() {
    registro = new SimpleMeterRegistry();
    listener = new NotificacionMetricasListener(registro);
  }

  private double cantidad(String estado) {
    return registro.find("donatrack.notificaciones").tag("estado", estado).counter().count();
  }

  @Test
  @DisplayName("Una notificación creada cuenta el estado pendiente")
  void creadaCuentaPendiente() {
    listener.onNotificacionCreada(new NotificacionCreada(id, UUID.randomUUID(), fecha));

    assertEquals(1, cantidad("pendiente"));
  }

  @Test
  @DisplayName("Una notificación enviada cuenta el estado enviada")
  void enviadaCuentaEnviada() {
    listener.onNotificacionEnviada(new NotificacionEnviada(id, fecha));

    assertEquals(1, cantidad("enviada"));
  }

  @Test
  @DisplayName("Una notificación fallida cuenta el estado fallida")
  void fallidaCuentaFallida() {
    listener.onNotificacionFallida(new NotificacionFallida(id, fecha));

    assertEquals(1, cantidad("fallida"));
  }
}
