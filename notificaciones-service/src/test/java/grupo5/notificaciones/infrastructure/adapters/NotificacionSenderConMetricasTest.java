package grupo5.notificaciones.infrastructure.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.notificaciones.infrastructure.NotificacionRouter;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NotificacionSenderConMetricasTest {

  private static final String MENSAJE = "Mensaje de prueba";

  private NotificacionRouter router;
  private SimpleMeterRegistry registro;
  private NotificacionSenderConMetricas sender;

  @BeforeEach
  void setUp() {
    router = mock(NotificacionRouter.class);
    registro = new SimpleMeterRegistry();
    sender = new NotificacionSenderConMetricas(router, registro);
  }

  private double envios(String canal, String resultado) {
    var busqueda = registro.find("donatrack.notificaciones.envios");
    return busqueda.tags("canal", canal, "resultado", resultado).counter().count();
  }

  private static Telefono telefono(TipoTelefono tipo) {
    Telefono telefono = new Telefono();
    telefono.setTipo(tipo);
    return telefono;
  }

  @ParameterizedTest
  @CsvSource({"true,enviado", "false,fallido"})
  @DisplayName("Un correo cuenta según el resultado del router y lo devuelve")
  void correoCuentaSegunResultado(boolean exito, String resultado) {
    Correo correo = new Correo();
    when(router.enviarA(correo, MENSAJE)).thenReturn(exito);

    assertEquals(exito, sender.enviarA(correo, MENSAJE));

    assertEquals(1, envios("correo", resultado));
  }

  @ParameterizedTest
  @CsvSource({"ESTANDAR,estandar", "WHATSAPP,whatsapp"})
  @DisplayName("Un teléfono cuenta con su tipo como canal, enviado o fallido")
  void telefonoCuentaPorTipo(TipoTelefono tipo, String canal) {
    Telefono telefono = telefono(tipo);
    when(router.enviarA(telefono, MENSAJE)).thenReturn(true, false);

    assertTrue(sender.enviarA(telefono, MENSAJE));
    assertFalse(sender.enviarA(telefono, MENSAJE));

    assertEquals(1, envios(canal, "enviado"));
    assertEquals(1, envios(canal, "fallido"));
  }

  @Test
  @DisplayName("Si el router lanza una excepción cuenta fallido y la relanza")
  void excepcionCuentaFallidoYRelanza() {
    Correo correo = new Correo();
    when(router.enviarA(correo, MENSAJE)).thenThrow(new IllegalStateException("caído"));

    assertThrows(IllegalStateException.class, () -> sender.enviarA(correo, MENSAJE));

    assertEquals(1, envios("correo", "fallido"));
  }
}
