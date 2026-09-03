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
import java.util.stream.IntStream;
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
    solicitud.procesarResultados(rutas, solicitud.getEntregaIds());
    rutas.clear();
    assertEquals(EstadoSolicitud.PROCESADA, solicitud.getEstado());
    assertEquals(1, solicitud.getRutasGeneradas().size());
    List<UUID> rutasGeneradas = solicitud.getRutasGeneradas();
    assertThrows(UnsupportedOperationException.class, rutasGeneradas::clear);
  }

  @Test
  void procesarResultadosRechazaNuloYEstadosQueNoSeanPendiente() {
    SolicitudPlanificacion pendiente = SolicitudPlanificacionMother.pendiente();
    assertThrows(
        ValidationException.class,
        () -> pendiente.procesarResultados(null, pendiente.getEntregaIds()));

    SolicitudPlanificacion enError = SolicitudPlanificacionMother.enError();
    List<UUID> rutasVacias = List.of();
    assertThrows(
        ValidationException.class,
        () -> enError.procesarResultados(rutasVacias, enError.getEntregaIds()));

    SolicitudPlanificacion procesada = SolicitudPlanificacionMother.procesada();
    assertThrows(
        ValidationException.class,
        () -> procesada.procesarResultados(rutasVacias, procesada.getEntregaIds()));
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

    SolicitudPlanificacion procesada = SolicitudPlanificacionMother.procesada();
    assertThrows(ValidationException.class, () -> procesada.marcarError("tarde"));
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
    SolicitudPlanificacion pendiente = SolicitudPlanificacionMother.pendiente();
    assertThrows(ValidationException.class, pendiente::reintentar);

    SolicitudPlanificacion procesada = SolicitudPlanificacionMother.procesada();
    assertThrows(ValidationException.class, procesada::reintentar);
  }

  @Test
  void constructorRechazaDatosInvalidos() {
    LocalDate fecha = LocalDate.now();
    String callback = "http://callback";
    List<UUID> entregas = List.of(UUID.randomUUID());
    List<UUID> camiones = List.of(UUID.randomUUID());
    List<UUID> choferes = List.of(UUID.randomUUID());
    assertThrows(
        ValidationException.class,
        () -> new SolicitudPlanificacion(null, entregas, camiones, choferes, callback));
    assertThrows(
        ValidationException.class,
        () -> new SolicitudPlanificacion(fecha, null, camiones, choferes, callback));
    assertThrows(
        ValidationException.class,
        () -> new SolicitudPlanificacion(fecha, entregas, camiones, choferes, null));
    assertThrows(
        ValidationException.class,
        () -> new SolicitudPlanificacion(fecha, entregas, camiones, choferes, " "));
    assertThrows(
        ValidationException.class,
        () -> new SolicitudPlanificacion(fecha, List.of(), camiones, choferes, callback));
    assertThrows(
        ValidationException.class,
        () ->
            new SolicitudPlanificacion(
                fecha,
                IntStream.rangeClosed(0, SolicitudPlanificacion.MAX_DONACIONES_POR_LOTE)
                    .mapToObj(indice -> UUID.randomUUID())
                    .toList(),
                camiones,
                choferes,
                callback));
  }

  @Test
  void procesarResultadoParcialConservaLasEntregasNoAsignadas() {
    UUID asignada = UUID.randomUUID();
    UUID pendiente = UUID.randomUUID();
    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            LocalDate.now(),
            List.of(asignada, pendiente),
            List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()),
            "http://callback");

    solicitud.procesarResultados(List.of(UUID.randomUUID()), List.of(asignada));

    assertEquals(EstadoSolicitud.PARCIAL, solicitud.getEstado());
    assertEquals(List.of(pendiente), solicitud.getEntregasNoAsignadas());
  }
}
