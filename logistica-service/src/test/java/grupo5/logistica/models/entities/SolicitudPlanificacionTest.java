package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.testutils.SolicitudPlanificacionMother;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudPlanificacionTest {
  @Test
  void iniciaPendienteYSinResultadosNiErrores() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
    assertEquals(0, solicitud.getIntentosFallidos());
    assertNull(solicitud.getMotivoError());
    assertTrue(solicitud.getRutasGeneradas().isEmpty());
  }

  @Test
  void procesarResultadosCopiaLasRutasYMarcaProcesada() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    List<UUID> rutas = new ArrayList<>(List.of(UUID.randomUUID()));
    solicitud.procesarResultados(rutas);
    rutas.clear();
    assertEquals(EstadoSolicitud.PROCESADA, solicitud.getEstado());
    assertEquals(1, solicitud.getRutasGeneradas().size());
    assertThrows(UnsupportedOperationException.class, () -> solicitud.getRutasGeneradas().clear());
  }

  @Test
  void procesarResultadosRechazaNuloYEstadosQueNoSeanPendiente() {
    SolicitudPlanificacion pendiente = SolicitudPlanificacionMother.pendiente();
    assertThrows(ValidationException.class, () -> pendiente.procesarResultados(null));
    assertThrows(
        ValidationException.class,
        () -> SolicitudPlanificacionMother.enError().procesarResultados(List.of()));
    assertThrows(
        ValidationException.class,
        () -> SolicitudPlanificacionMother.procesada().procesarResultados(List.of()));
  }

  @Test
  void marcarErrorRegistraMotivoEIncrementaIntentos() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    solicitud.marcarError("timeout");
    solicitud.marcarError("segundo timeout");
    assertEquals(EstadoSolicitud.ERROR, solicitud.getEstado());
    assertEquals(2, solicitud.getIntentosFallidos());
    assertEquals("segundo timeout", solicitud.getMotivoError());
  }

  @Test
  void marcarErrorRechazaMotivosInvalidosYUnaSolicitudProcesada() {
    SolicitudPlanificacion pendiente = SolicitudPlanificacionMother.pendiente();
    assertThrows(ValidationException.class, () -> pendiente.marcarError(null));
    assertThrows(ValidationException.class, () -> pendiente.marcarError("  "));
    assertThrows(
        ValidationException.class,
        () -> SolicitudPlanificacionMother.procesada().marcarError("tarde"));
  }

  @Test
  void reintentarSobreErrorVuelveAPendienteYConservaAuditoria() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.enError();
    solicitud.reintentar();
    assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
    assertEquals(1, solicitud.getIntentosFallidos());
    assertEquals("Falla del proveedor", solicitud.getMotivoError());
  }

  @Test
  void reintentarRechazaEstadosQueNoSeanError() {
    assertThrows(
        ValidationException.class, () -> SolicitudPlanificacionMother.pendiente().reintentar());
    assertThrows(
        ValidationException.class, () -> SolicitudPlanificacionMother.procesada().reintentar());
  }

  @Test
  void constructorRechazaDatosInvalidos() {
    LocalDate fecha = LocalDate.now();
    String callback = "http://callback";
    assertThrows(ValidationException.class, () -> new SolicitudPlanificacion(null, 1, callback));
    assertThrows(
        ValidationException.class, () -> new SolicitudPlanificacion(fecha, null, callback));
    assertThrows(ValidationException.class, () -> new SolicitudPlanificacion(fecha, 1, null));
    assertThrows(ValidationException.class, () -> new SolicitudPlanificacion(fecha, 1, " "));
    assertThrows(ValidationException.class, () -> new SolicitudPlanificacion(fecha, 0, callback));
    assertThrows(
        ValidationException.class,
        () ->
            new SolicitudPlanificacion(
                fecha, SolicitudPlanificacion.MAX_DONACIONES_POR_LOTE + 1, callback));
  }
}
