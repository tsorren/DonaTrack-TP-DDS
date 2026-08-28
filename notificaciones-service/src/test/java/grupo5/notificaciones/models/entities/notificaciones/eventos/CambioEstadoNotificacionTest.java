package grupo5.notificaciones.models.entities.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;

/**
 * Cubre la construcción de {@code CambioEstadoNotificacion} (Oleada 8) — no existía ningún test
 * dedicado antes de esta oleada. La inmutabilidad de sus 3 campos ({@code final}, sin setters) se
 * verifica indirectamente: no hay ningún método para mutarlos después de construir la instancia,
 * así que no hay nada que un test pueda "intentar romper" — se deja constancia leyendo los 3
 * valores tal cual quedaron seteados.
 */
class CambioEstadoNotificacionTest {

  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, Month.JULY, 2, 12, 0, 0);

  @Test
  void constructor_deberiaAsignarLosTresCamposTalCualSePasaron() {
    CambioEstadoNotificacion cambio =
        new CambioEstadoNotificacion(
            EstadoNotificacion.PENDIENTE, EstadoNotificacion.ENVIADA, TEST_DATE_TIME);

    assertEquals(EstadoNotificacion.PENDIENTE, cambio.getEstadoAnterior());
    assertEquals(EstadoNotificacion.ENVIADA, cambio.getEstadoNuevo());
    assertEquals(TEST_DATE_TIME, cambio.getTimestamp());
  }

  @Test
  void constructor_conEstadoAnteriorNulo_deberiaAceptarloTalCual() {
    // Notificacion.actualizarEstado() pasa "anterior" como null en la primera transición
    // (construcción) — este caso real se cubre explícitamente acá, no solo indirectamente.
    CambioEstadoNotificacion cambio =
        new CambioEstadoNotificacion(null, EstadoNotificacion.PENDIENTE, TEST_DATE_TIME);

    assertNull(cambio.getEstadoAnterior());
    assertEquals(EstadoNotificacion.PENDIENTE, cambio.getEstadoNuevo());
  }
}
