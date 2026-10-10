package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    List<UUID> rutasGeneradas = solicitud.getRutasGeneradas();
    assertThrows(UnsupportedOperationException.class, rutasGeneradas::clear);
  }

  @Test
  void procesarResultadosRechazaNuloYEstadosQueNoSeanPendiente() {
    SolicitudPlanificacion pendiente = SolicitudPlanificacionMother.pendiente();
    assertThrows(ValidationException.class, () -> pendiente.procesarResultados(null));

    SolicitudPlanificacion enError = SolicitudPlanificacionMother.enError();
    List<UUID> rutasVacias = List.of();
    assertThrows(ValidationException.class, () -> enError.procesarResultados(rutasVacias));

    SolicitudPlanificacion procesada = SolicitudPlanificacionMother.procesada();
    assertThrows(ValidationException.class, () -> procesada.procesarResultados(rutasVacias));
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

  // ========================= RECONSTITUCIÓN (JPA) =========================

  @Test
  void testConstructorReconstitucionExitoso() {
    UUID id = UUID.randomUUID();
    LocalDate fecha = LocalDate.now();
    UUID rutaId = UUID.randomUUID();
    List<UUID> rutas = new ArrayList<>(List.of(rutaId));

    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            id,
            fecha,
            EstadoSolicitud.ERROR,
            25,
            "http://callback-url.com",
            rutas,
            2,
            "Timeout proveedor",
            3L);

    assertEquals(id, solicitud.getId());
    assertEquals(fecha, solicitud.getFecha());
    assertEquals(EstadoSolicitud.ERROR, solicitud.getEstado());
    assertEquals(25, solicitud.getCantidadDonaciones());
    assertEquals("http://callback-url.com", solicitud.getCallbackUrl());
    assertEquals(1, solicitud.getRutasGeneradas().size());
    assertEquals(rutaId, solicitud.getRutasGeneradas().get(0));
    assertEquals(2, solicitud.getIntentosFallidos());
    assertEquals("Timeout proveedor", solicitud.getMotivoError());
    assertEquals(3L, solicitud.getVersion());
  }

  @Test
  void testConstructorReconstitucionConColeccionNulaNoFalla() {
    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(),
            LocalDate.now(),
            EstadoSolicitud.PENDIENTE,
            10,
            "http://callback-url.com",
            null,
            0,
            null,
            1L);

    assertNotNull(solicitud.getRutasGeneradas());
    assertTrue(solicitud.getRutasGeneradas().isEmpty());
    assertEquals(1L, solicitud.getVersion());
  }

  @Test
  void testConstructorNegocioMantieneVersionEnNull() {
    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(LocalDate.now(), 10, "http://callback-url.com");
    assertNull(solicitud.getVersion());
  }

  @Test
  void testConstructorReconstitucionAislaColeccionOriginal() {
    UUID rutaId = UUID.randomUUID();
    List<UUID> rutasOriginal = new ArrayList<>();
    rutasOriginal.add(rutaId);

    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(),
            LocalDate.now(),
            EstadoSolicitud.PROCESADA,
            10,
            "http://callback-url.com",
            rutasOriginal,
            0,
            null,
            1L);

    assertEquals(1, solicitud.getRutasGeneradas().size());
    rutasOriginal.add(UUID.randomUUID());
    assertEquals(1, solicitud.getRutasGeneradas().size());
  }
}
