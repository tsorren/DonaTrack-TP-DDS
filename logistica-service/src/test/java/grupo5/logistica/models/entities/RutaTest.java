package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.CambioEstadoRuta;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.eventos.EventoRuta;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaIniciada;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RutaTest {

  @Test
  void testConstructorExitoso() {
    LocalDate fecha = LocalDate.now();
    UUID choferId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();

    Ruta ruta = new Ruta(fecha, choferId, camionId);

    assertNotNull(ruta.getId());
    assertEquals(fecha, ruta.getFecha());
    assertEquals(choferId, ruta.getChoferId());
    assertEquals(camionId, ruta.getCamionId());
    assertEquals(EstadoRuta.PENDIENTE, ruta.getEstado());
    assertTrue(ruta.getEntregaIds().isEmpty());
    assertFalse(ruta.tieneSeguimientoDisponible());
  }

  @Test
  void testConstructorConFechaNulaLanzaExcepcion() {
    UUID choferId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();

    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Ruta(null, choferId, camionId));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testConstructorConChoferNuloLanzaExcepcion() {
    LocalDate fecha = LocalDate.now();
    UUID camionId = UUID.randomUUID();
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Ruta(fecha, null, camionId));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testAgregarEntregaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();

    ruta.agregarEntrega(entregaId);

    assertEquals(1, ruta.getEntregaIds().size());
    assertTrue(ruta.getEntregaIds().contains(entregaId));
  }

  @Test
  void testAgregarEntregaNulaLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());

    ValidationException exception =
        assertThrows(ValidationException.class, () -> ruta.agregarEntrega(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testAgregarEntregaDuplicadaLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();
    ruta.agregarEntrega(entregaId);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> ruta.agregarEntrega(entregaId));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testIniciarRutaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    ruta.iniciarRuta();

    assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
    assertNotNull(ruta.getHoraInicioReal());
    assertNull(ruta.getHoraFinReal());
    assertTrue(ruta.tieneSeguimientoDisponible());
  }

  @Test
  void testIniciarRutaSinEntregasLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, ruta::iniciarRuta);
    assertEquals(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testCompletarRutaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());
    ruta.iniciarRuta();

    ruta.completarRuta();

    assertEquals(EstadoRuta.COMPLETADA, ruta.getEstado());
    assertNotNull(ruta.getHoraFinReal());
    assertTrue(ruta.tieneSeguimientoDisponible());
  }

  @Test
  void testCompletarRutaCuandoEstaPendienteLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, ruta::completarRuta);
    assertEquals(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testColeccionEntregasInmutable() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    List<UUID> entregas = ruta.getEntregaIds();
    UUID nuevaEntregaId = UUID.randomUUID();
    assertThrows(UnsupportedOperationException.class, () -> entregas.add(nuevaEntregaId));
  }

  @Test
  void agregarEntregaRegistraEventoDeDominio() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();

    ruta.agregarEntrega(entregaId);

    EventoRutaAsignada evento =
        assertInstanceOf(EventoRutaAsignada.class, ruta.getDomainEvents().getFirst());
    assertEquals(ruta.getId(), evento.getRutaId());
    assertEquals(entregaId, evento.getEntregaId());
    assertNotNull(evento.getId());
    assertNotNull(evento.getTimestamp());
  }

  @Test
  void iniciarRutaRegistraEventoConSnapshotDeEntregas() {
    UUID camionId = UUID.randomUUID();
    UUID entregaId = UUID.randomUUID();
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), camionId);
    ruta.agregarEntrega(entregaId);
    ruta.clearDomainEvents();

    ruta.iniciarRuta();

    EventoRutaIniciada evento =
        assertInstanceOf(EventoRutaIniciada.class, ruta.getDomainEvents().getFirst());
    assertEquals(ruta.getId(), evento.getRutaId());
    assertEquals(camionId, evento.getCamionId());
    List<UUID> entregaIds = evento.getEntregaIds();
    UUID nuevoId = UUID.randomUUID();
    assertThrows(UnsupportedOperationException.class, () -> entregaIds.add(nuevoId));
  }

  @Test
  void snapshotDeEventosEsInmutableYNoCambiaAlLimpiarLaRuta() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());
    List<EventoRuta> snapshot = ruta.getDomainEvents();

    ruta.clearDomainEvents();

    assertEquals(1, snapshot.size());
    assertTrue(ruta.getDomainEvents().isEmpty());
    EventoRuta primerEvento = snapshot.getFirst();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(primerEvento));
  }

  // ========================= RECONSTITUCIÓN (JPA) =========================

  @Test
  void testConstructorReconstitucionExitoso() {
    UUID id = UUID.randomUUID();
    LocalDate fecha = LocalDate.now();
    UUID entregaId = UUID.randomUUID();
    List<UUID> entregas = new ArrayList<>(List.of(entregaId));
    UUID choferId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();
    LocalDateTime inicio = LocalDateTime.now().minusHours(3);
    LocalDateTime fin = LocalDateTime.now().minusHours(1);
    LocalDateTime timestamp = LocalDateTime.now().minusHours(3);
    CambioEstadoRuta cambio =
        new CambioEstadoRuta(EstadoRuta.PENDIENTE, EstadoRuta.EN_TRASLADO, timestamp);
    List<CambioEstadoRuta> historial = new ArrayList<>(List.of(cambio));

    Ruta ruta =
        new Ruta(
            id,
            fecha,
            entregas,
            choferId,
            camionId,
            EstadoRuta.COMPLETADA,
            historial,
            inicio,
            fin,
            5L);

    assertEquals(id, ruta.getId());
    assertEquals(fecha, ruta.getFecha());
    assertEquals(1, ruta.getEntregaIds().size());
    assertEquals(entregaId, ruta.getEntregaIds().get(0));
    assertEquals(choferId, ruta.getChoferId());
    assertEquals(camionId, ruta.getCamionId());
    assertEquals(EstadoRuta.COMPLETADA, ruta.getEstado());
    assertEquals(1, ruta.getHistorialEstado().size());
    assertEquals(cambio, ruta.getHistorialEstado().get(0));
    assertEquals(inicio, ruta.getHoraInicioReal());
    assertEquals(fin, ruta.getHoraFinReal());
    assertEquals(5L, ruta.getVersion());
  }

  @Test
  void testConstructorReconstitucionConColeccionesNulasNoFalla() {
    Ruta ruta =
        new Ruta(
            UUID.randomUUID(),
            LocalDate.now(),
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            EstadoRuta.PENDIENTE,
            null,
            null,
            null,
            1L);

    assertNotNull(ruta.getEntregaIds());
    assertTrue(ruta.getEntregaIds().isEmpty());
    assertNotNull(ruta.getHistorialEstado());
    assertTrue(ruta.getHistorialEstado().isEmpty());
    assertEquals(1L, ruta.getVersion());
  }

  @Test
  void testConstructorNegocioMantieneVersionEnNull() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    assertNull(ruta.getVersion());
  }

  @Test
  void testConstructorReconstitucionAislaColeccionesOriginales() {
    UUID entregaId = UUID.randomUUID();
    List<UUID> entregasOriginal = new ArrayList<>();
    entregasOriginal.add(entregaId);

    LocalDateTime timestamp = LocalDateTime.now();
    CambioEstadoRuta cambio =
        new CambioEstadoRuta(EstadoRuta.PENDIENTE, EstadoRuta.EN_TRASLADO, timestamp);
    List<CambioEstadoRuta> historialOriginal = new ArrayList<>();
    historialOriginal.add(cambio);

    Ruta ruta =
        new Ruta(
            UUID.randomUUID(),
            LocalDate.now(),
            entregasOriginal,
            UUID.randomUUID(),
            UUID.randomUUID(),
            EstadoRuta.PENDIENTE,
            historialOriginal,
            null,
            null,
            1L);

    assertEquals(1, ruta.getEntregaIds().size());
    assertEquals(1, ruta.getHistorialEstado().size());

    entregasOriginal.add(UUID.randomUUID());
    historialOriginal.add(
        new CambioEstadoRuta(EstadoRuta.EN_TRASLADO, EstadoRuta.COMPLETADA, timestamp));

    assertEquals(1, ruta.getEntregaIds().size());
    assertEquals(1, ruta.getHistorialEstado().size());
  }
}
