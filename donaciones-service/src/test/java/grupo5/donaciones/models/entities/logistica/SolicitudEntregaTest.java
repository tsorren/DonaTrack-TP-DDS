package grupo5.donaciones.models.entities.logistica;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudEntregaTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 7, 12, 0);

  private static SolicitudEntrega nueva() {
    return new SolicitudEntrega(UUID.randomUUID(), AHORA);
  }

  @Test
  void nueva_deberiaQuedarPendienteSinProveedor() {
    SolicitudEntrega solicitud = nueva();

    assertEquals(EstadoSolicitudEntrega.PENDIENTE, solicitud.getEstado());
    assertNull(solicitud.getProveedorActual());
    assertTrue(solicitud.getProveedoresDescartados().isEmpty());
    assertTrue(solicitud.estaActiva());
  }

  @Test
  void nueva_deberiaRechazarDatosNulos() {
    UUID donacionId = UUID.randomUUID();

    assertThrows(ValidationException.class, () -> new SolicitudEntrega(null, AHORA));
    assertThrows(ValidationException.class, () -> new SolicitudEntrega(donacionId, null));
  }

  @Test
  void marcarEnviada_deberiaPasarAEnviada_CuandoTieneProveedor() {
    SolicitudEntrega solicitud = nueva();
    solicitud.asignarProveedor("donatrack");

    solicitud.marcarEnviada();

    assertEquals(EstadoSolicitudEntrega.ENVIADA, solicitud.getEstado());
    assertTrue(solicitud.estaActiva());
  }

  @Test
  void marcarEnviada_deberiaFallar_CuandoNoTieneProveedor() {
    SolicitudEntrega solicitud = nueva();

    assertThrows(BusinessStateException.class, solicitud::marcarEnviada);
  }

  @Test
  void descartarProveedorActual_deberiaRecordarloYLiberarElLugar() {
    SolicitudEntrega solicitud = nueva();
    solicitud.asignarProveedor("donatrack");

    solicitud.descartarProveedorActual();

    assertNull(solicitud.getProveedorActual());
    assertTrue(solicitud.fueDescartado("donatrack"));
    assertEquals(EstadoSolicitudEntrega.PENDIENTE, solicitud.getEstado());
  }

  @Test
  void asignarProveedor_deberiaRechazarUnProveedorYaDescartado() {
    SolicitudEntrega solicitud = nueva();
    solicitud.asignarProveedor("donatrack");
    solicitud.descartarProveedorActual();

    assertThrows(BusinessStateException.class, () -> solicitud.asignarProveedor("donatrack"));
  }

  @Test
  void asignarProveedor_deberiaRechazarSiYaHayUnoAsignado() {
    SolicitudEntrega solicitud = nueva();
    solicitud.asignarProveedor("donatrack");

    assertThrows(BusinessStateException.class, () -> solicitud.asignarProveedor("externo"));
  }

  @Test
  void asignarProveedor_deberiaRechazarIdVacio() {
    SolicitudEntrega solicitud = nueva();

    assertThrows(ValidationException.class, () -> solicitud.asignarProveedor(" "));
  }

  @Test
  void marcarFallida_deberiaDejarDeEstarActiva() {
    SolicitudEntrega solicitud = nueva();

    solicitud.marcarFallida();

    assertEquals(EstadoSolicitudEntrega.FALLIDA, solicitud.getEstado());
    assertFalse(solicitud.estaActiva());
  }

  @Test
  void deberiaRechazarCualquierOperacion_CuandoYaNoEstaPendiente() {
    SolicitudEntrega enviada = nueva();
    enviada.asignarProveedor("donatrack");
    enviada.marcarEnviada();
    SolicitudEntrega fallida = nueva();
    fallida.marcarFallida();

    assertThrows(BusinessStateException.class, enviada::marcarFallida);
    assertThrows(BusinessStateException.class, enviada::descartarProveedorActual);
    assertThrows(BusinessStateException.class, fallida::marcarEnviada);
    assertThrows(BusinessStateException.class, () -> fallida.asignarProveedor("externo"));
  }

  @Test
  void getProveedoresDescartados_noDeberiaPermitirModificacionesExternas() {
    Set<String> descartados = nueva().getProveedoresDescartados();

    assertThrows(UnsupportedOperationException.class, () -> descartados.add("x"));
  }
}
