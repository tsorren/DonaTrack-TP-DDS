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

    // Negar entrega
    entrega.negarEntrega("Comedor Infantil", "Domicilio cerrado", true);
    assertEquals(
        EstadoEntrega.REVISION,
        entrega.getEstadoActual()); // Pasa a NO_RECIBIDA y luego inmediatamente a REVISION en
    // negarEntrega()
    assertEquals(
        3,
        entrega
            .getHistorialEstado()
            .size()); // Registro de EN_TRASLADO -> NO_RECIBIDA y NO_RECIBIDA -> REVISION

    // Regresar al deposito
    entrega.regresarAlDeposito("Admin Carlos");
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
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
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(snapshot.getFirst()));
  }
}
