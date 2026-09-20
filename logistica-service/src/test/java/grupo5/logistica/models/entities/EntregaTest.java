package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.entregas.eventos.EventoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntregaTest {

  private Direccion createTestDireccion() {
    Pais pais = new Pais("Argentina");
    Provincia prov = new Provincia("Buenos Aires", pais);
    Localidad loc = new Localidad("Lanus", prov);
    return new Direccion("Calle Falsa", 123, null, null, "1824", loc);
  }

  private Entrega crearEntregaValida() {
    return new Entrega(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), createTestDireccion(), 10f, 1f);
  }

  @Test
  void testConstructorExitoso() {
    UUID idRuta = UUID.randomUUID();
    UUID idDonacion = UUID.randomUUID();
    UUID idBeneficiaria = UUID.randomUUID();
    Direccion destino = createTestDireccion();

    Entrega entrega = new Entrega(idRuta, idDonacion, idBeneficiaria, destino, 10.5f, 0.5f);

    assertNotNull(entrega.getId());
    assertEquals(idRuta, entrega.getIdRuta());
    assertEquals(idDonacion, entrega.getIdDonacion());
    assertEquals(idBeneficiaria, entrega.getIdBeneficiaria());
    assertEquals(destino, entrega.getDestino());
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
    assertEquals(10.5f, entrega.getPesoTotalKG());
    assertEquals(0.5f, entrega.getVolumenTotalM3());
    assertTrue(entrega.getHistorialEstado().isEmpty());
  }

  @Test
  void testConstructorConIdDonacionNuloLanzaExcepcion() {
    UUID idRuta = UUID.randomUUID();
    UUID idBeneficiaria = UUID.randomUUID();
    Direccion destino = createTestDireccion();
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new Entrega(idRuta, null, idBeneficiaria, destino, 10f, 1f));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testConstructorConPesoNegativoLanzaExcepcion() {
    UUID idRuta = UUID.randomUUID();
    UUID idDonacion = UUID.randomUUID();
    UUID idBeneficiaria = UUID.randomUUID();
    Direccion destino = createTestDireccion();
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new Entrega(idRuta, idDonacion, idBeneficiaria, destino, -5f, 1f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testFlujoFelizDeTrazabilidad() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    // 1. Iniciar ruta
    entrega.iniciarRuta("Chofer Jose");
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstadoActual());
    assertNotNull(entrega.getHoraSalida());
    assertEquals(1, entrega.getHistorialEstado().size());
    CambioEstadoEntrega cambio1 = entrega.getHistorialEstado().getFirst();
    assertEquals(EstadoEntrega.PENDIENTE, cambio1.estadoAnterior());
    assertEquals(EstadoEntrega.EN_TRASLADO, cambio1.estadoNuevo());
    assertEquals("Chofer Jose", cambio1.actor());

    // 2. Confirmar entrega
    entrega.confirmarEntrega("Comedor Infantil");
    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertNotNull(entrega.getHoraArribo());
    assertEquals(2, entrega.getHistorialEstado().size());
    CambioEstadoEntrega cambio2 = entrega.getHistorialEstado().get(1);
    assertEquals(EstadoEntrega.EN_TRASLADO, cambio2.estadoAnterior());
    assertEquals(EstadoEntrega.ENTREGADA, cambio2.estadoNuevo());
    assertEquals("Comedor Infantil", cambio2.actor());

    // 3. Adjuntar foto
    entrega.adjuntarFotoRecepcion("http://images.com/recepcion.jpg");
    assertEquals("http://images.com/recepcion.jpg", entrega.getFotoRecepcionUrl());
  }

  @Test
  void testFlujoAlternativoNoRecibidoYRegreso() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    entrega.iniciarRuta("Chofer Jose");

    // 1. Negar entrega
    entrega.negarEntrega("Comedor Infantil", "Domicilio cerrado", true);
    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstadoActual());
    assertEquals(
        2,
        entrega
            .getHistorialEstado()
            .size()); // PENDIENTE -> EN_TRASLADO, EN_TRASLADO -> NO_RECIBIDA
    // 2. Administrador toma el caso y lo pasa a revisión
    entrega.mandarARevision("Admin Carlos");
    assertEquals(EstadoEntrega.REVISION, entrega.getEstadoActual());
    assertEquals(3, entrega.getHistorialEstado().size()); // NO_RECIBIDA -> REVISION
    // 3. Regresar al depósito
    entrega.regresarAlDeposito("Admin Carlos");
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
    assertEquals(4, entrega.getHistorialEstado().size()); // REVISION -> PENDIENTE
    assertNull(entrega.getHoraArribo());
    assertNull(entrega.getHoraSalida());
  }

  @Test
  void testIniciarRutaConChoferVacioLanzaExcepcion() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> entrega.iniciarRuta(" "));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testTransicionInvalidaLanzaExcepcion() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    // Confirmar entrega directamente sin iniciar ruta
    ValidationException exception =
        assertThrows(ValidationException.class, () -> entrega.confirmarEntrega("Comedor"));
    assertEquals(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testHistorialEstadoEsInmutable() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);
    entrega.iniciarRuta("Chofer Jose");

    List<CambioEstadoEntrega> historial = entrega.getHistorialEstado();
    CambioEstadoEntrega nuevoCambio =
        new CambioEstadoEntrega(EstadoEntrega.PENDIENTE, EstadoEntrega.ENTREGADA, null, "hack");
    assertThrows(UnsupportedOperationException.class, () -> historial.add(nuevoCambio));
  }

  @Test
  void confirmarEntregaRegistraEventoDeDominio() {
    UUID rutaId = UUID.randomUUID();
    UUID donacionId = UUID.randomUUID();
    Entrega entrega =
        new Entrega(rutaId, donacionId, UUID.randomUUID(), createTestDireccion(), 10f, 1f);
    entrega.iniciarRuta("Chofer Jose");

    entrega.confirmarEntrega("Comedor Infantil");

    EntregaConfirmada evento =
        assertInstanceOf(EntregaConfirmada.class, entrega.getDomainEvents().getFirst());
    assertEquals(entrega.getId(), evento.getEntregaId());
    assertEquals(donacionId, evento.getDonacionId());
    assertEquals(rutaId, evento.getIdRuta());
    assertNotNull(evento.getId());
    assertNotNull(evento.getTimestamp());
  }

  @Test
  void negarEntregaRegistraEventoDeDominio() {
    UUID donacionId = UUID.randomUUID();
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(), donacionId, UUID.randomUUID(), createTestDireccion(), 10f, 1f);
    entrega.iniciarRuta("Chofer Jose");

    entrega.negarEntrega("Comedor Infantil", "Domicilio cerrado", false);

    EntregaFallida evento =
        assertInstanceOf(EntregaFallida.class, entrega.getDomainEvents().getFirst());
    assertEquals(entrega.getId(), evento.getEntregaId());
    assertEquals(donacionId, evento.getDonacionId());
    assertEquals("Domicilio cerrado", evento.getJustificacion());
    assertFalse(evento.isReplanificable());
    assertNotNull(evento.getId());
    assertNotNull(evento.getTimestamp());
  }

  @Test
  void snapshotDeEventosEsInmutableYNoCambiaAlLimpiarElAgregado() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);
    entrega.iniciarRuta("Chofer Jose");
    entrega.confirmarEntrega("Comedor Infantil");
    List<EventoEntrega> snapshot = entrega.getDomainEvents();

    entrega.clearDomainEvents();

    assertEquals(1, snapshot.size());
    assertInstanceOf(EntregaConfirmada.class, snapshot.getFirst());
    assertTrue(entrega.getDomainEvents().isEmpty());
    EventoEntrega primerEvento = snapshot.getFirst();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(primerEvento));
  }

  @Test
  void testRegresarAlDepositoDirectamenteDesdeNoRecibidaLanzaExcepcion() {
    Entrega entrega = crearEntregaValida();
    entrega.iniciarRuta("Chofer Jose");
    entrega.negarEntrega("Comedor Infantil", "Domicilio cerrado", true);

    // Debe fallar porque no pasó por REVISION
    ValidationException ex =
        assertThrows(ValidationException.class, () -> entrega.regresarAlDeposito("Admin Carlos"));
    assertEquals(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA, ex.getError());
  }

  @Test
  void testNegarEntregaSinJustificacionLanzaExcepcion() {
    Entrega entrega = crearEntregaValida();
    entrega.iniciarRuta("Chofer Jose");

    // Justificación nula
    assertThrows(ValidationException.class, () -> entrega.negarEntrega("Comedor", null, true));

    // Justificación vacía o en blanco
    assertThrows(ValidationException.class, () -> entrega.negarEntrega("Comedor", "   ", true));
  }

  // ========================= RECONSTITUCIÓN (JPA) =========================

  @Test
  void testConstructorReconstitucionExitoso() {
    UUID id = UUID.randomUUID();
    UUID idRuta = UUID.randomUUID();
    UUID idDonacion = UUID.randomUUID();
    UUID idBeneficiaria = UUID.randomUUID();
    Direccion destino = createTestDireccion();
    LocalDateTime horaSalida = LocalDateTime.now().minusHours(2);
    LocalDateTime horaArribo = LocalDateTime.now().minusHours(1);
    LocalDateTime timestamp = LocalDateTime.now().minusHours(2);
    CambioEstadoEntrega cambio =
        new CambioEstadoEntrega(
            EstadoEntrega.PENDIENTE, EstadoEntrega.EN_TRASLADO, timestamp, "Chofer Juan");
    List<CambioEstadoEntrega> historial = new ArrayList<>(List.of(cambio));

    Entrega entrega =
        new Entrega(
            id,
            idRuta,
            idDonacion,
            idBeneficiaria,
            destino,
            EstadoEntrega.EN_TRASLADO,
            historial,
            horaArribo,
            horaSalida,
            "http://foto.jpg",
            12.5f,
            1.8f,
            4L);

    assertEquals(id, entrega.getId());
    assertEquals(idRuta, entrega.getIdRuta());
    assertEquals(idDonacion, entrega.getIdDonacion());
    assertEquals(idBeneficiaria, entrega.getIdBeneficiaria());
    assertEquals(destino, entrega.getDestino());
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstadoActual());
    assertEquals(1, entrega.getHistorialEstado().size());
    assertEquals(cambio, entrega.getHistorialEstado().get(0));
    assertEquals(horaArribo, entrega.getHoraArribo());
    assertEquals(horaSalida, entrega.getHoraSalida());
    assertEquals("http://foto.jpg", entrega.getFotoRecepcionUrl());
    assertEquals(12.5f, entrega.getPesoTotalKG());
    assertEquals(1.8f, entrega.getVolumenTotalM3());
    assertEquals(4L, entrega.getVersion());
  }

  @Test
  void testConstructorReconstitucionConColeccionNulaNoFalla() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            EstadoEntrega.PENDIENTE,
            null,
            null,
            null,
            null,
            10f,
            1f,
            1L);

    assertNotNull(entrega.getHistorialEstado());
    assertTrue(entrega.getHistorialEstado().isEmpty());
    assertEquals(1L, entrega.getVersion());
  }

  @Test
  void testConstructorNegocioMantieneVersionEnNull() {
    Entrega entrega = crearEntregaValida();
    assertNull(entrega.getVersion());
  }

  @Test
  void testConstructorReconstitucionAislaColeccionOriginal() {
    LocalDateTime timestamp = LocalDateTime.now();
    CambioEstadoEntrega cambio =
        new CambioEstadoEntrega(
            EstadoEntrega.PENDIENTE, EstadoEntrega.EN_TRASLADO, timestamp, "Chofer Juan");
    List<CambioEstadoEntrega> historialOriginal = new ArrayList<>();
    historialOriginal.add(cambio);

    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            EstadoEntrega.PENDIENTE,
            historialOriginal,
            null,
            null,
            null,
            10f,
            1f,
            1L);

    assertEquals(1, entrega.getHistorialEstado().size());
    historialOriginal.add(
        new CambioEstadoEntrega(
            EstadoEntrega.EN_TRASLADO, EstadoEntrega.ENTREGADA, timestamp, "Receptor Pedro"));

    assertEquals(1, entrega.getHistorialEstado().size());
  }
}
