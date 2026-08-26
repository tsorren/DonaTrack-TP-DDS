package grupo5.donaciones.models.entities.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.events.EventoDeDominio;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.models.entities.donaciones.events.DonacionCargada;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.models.entities.donaciones.events.DonacionSegmentada;
import grupo5.donaciones.models.entities.donaciones.events.EventoDonacion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionTest {

  private UUID donanteId;
  private Donacion donacion;

  @BeforeEach
  void setUp() {
    donanteId = UUID.randomUUID();
    donacion = DonacionMother.simple(donanteId);
  }

  @Test
  void crearDonacion_conDonanteNulo_debeLanzarExcepcion() {
    ValidationException ex = assertThrows(ValidationException.class, () -> new Donacion(null));
    assertEquals(ErrorCatalog.DONACION_SIN_DONANTE, ex.getError());
  }

  @Test
  void crearDonacion_conDatosValidos_debeEstarEnEstadoCargadaYRegistrarDonacionCargada() {
    assertEquals(EstadoDonacion.CARGADA, donacion.getEstadoActual());
    assertEquals(donanteId, donacion.getDonanteId());
    assertNotNull(donacion.getId());
    assertEquals(1, donacion.getDomainEvents().size());

    EventoDonacion event = donacion.getDomainEvents().getFirst();
    assertInstanceOf(DonacionCargada.class, event);
    assertInstanceOf(EventoDeDominio.class, event);
    assertEquals(donacion.getId(), event.getDonacionId());
    assertEquals(donanteId, event.getDonanteId());
    assertNotNull(event.getId());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void agregarItem_conItemValido_debeAgregarALaLista() {
    Bien bien = BienMother.mueble("Mesa de madera");
    ItemDonacion item = new ItemDonacion(bien, 2);

    donacion.agregarItem(item);

    assertEquals(1, donacion.getItems().size());
    assertEquals(item, donacion.getItems().getFirst());
  }

  @Test
  void agregarItem_conItemNulo_debeLanzarExcepcion() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> donacion.agregarItem(null));
    assertEquals(ErrorCatalog.DONACION_ITEM_NULO, ex.getError());
  }

  @Test
  void agregarItem_conItemYaAgregado_debeLanzarExcepcion() {
    Bien bien = BienMother.mueble("Mesa de madera");
    ItemDonacion item = new ItemDonacion(bien, 2);
    donacion.agregarItem(item);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> donacion.agregarItem(item));
    assertEquals(ErrorCatalog.DONACION_ITEM_YA_AGREGADO, ex.getError());
  }

  @Test
  void quitarItem_conItemExistente_debeRemoverDeLista() {
    Bien bien = BienMother.mueble("Mesa de madera");
    ItemDonacion item = new ItemDonacion(bien, 2);
    donacion.agregarItem(item);
    assertEquals(1, donacion.getItems().size());

    donacion.quitarItem(item);
    assertEquals(0, donacion.getItems().size());
  }

  @Test
  void quitarItem_conItemNoExistente_debeLanzarExcepcion() {
    Bien bien = BienMother.mueble("Mesa de madera");
    ItemDonacion item = new ItemDonacion(bien, 2);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> donacion.quitarItem(item));
    assertEquals(ErrorCatalog.DONACION_ITEM_NO_PERTENECE, ex.getError());
  }

  @Test
  void marcarNormalizada_cuandoEstaCargada_debeAvanzarEstadoYRegistrarEvento() {
    donacion.clearDomainEvents();

    donacion.marcarNormalizada();

    assertEquals(EstadoDonacion.NORMALIZADA, donacion.getEstadoActual());
    assertEquals(1, donacion.getDomainEvents().size());
    EventoDonacion event = donacion.getDomainEvents().getFirst();
    assertInstanceOf(DonacionNormalizada.class, event);
    assertEquals(donacion.getId(), event.donacionId());
    assertEquals(donanteId, event.donanteId());
    assertEquals(1, donacion.getHistorialEstados().size());
  }

  @Test
  void marcarNormalizada_cuandoYaEstaNormalizada_debeLanzarExcepcion() {
    donacion.marcarNormalizada();

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, donacion::marcarNormalizada);
    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, ex.getError());
  }

  @Test
  void marcarSegmentada_cuandoEstaNormalizada_debeAvanzarEstadoYRegistrarEvento() {
    donacion.marcarNormalizada();
    donacion.clearDomainEvents();

    donacion.marcarSegmentada();

    assertEquals(EstadoDonacion.SEGMENTADA, donacion.getEstadoActual());
    assertEquals(1, donacion.getDomainEvents().size());
    EventoDonacion event = donacion.getDomainEvents().getFirst();
    assertInstanceOf(DonacionSegmentada.class, event);
    assertEquals(donacion.getId(), event.donacionId());
    assertEquals(donanteId, event.donanteId());
    assertEquals(2, donacion.getHistorialEstados().size());
  }

  @Test
  void marcarSegmentada_cuandoEstaEnCargada_debeLanzarExcepcion() {
    BusinessStateException ex =
        assertThrows(BusinessStateException.class, donacion::marcarSegmentada);
    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, ex.getError());
  }

  @Test
  void getDomainEvents_debeRetornarListaInmodificable() {
    List<EventoDonacion> events = donacion.getDomainEvents();
    assertThrows(
        UnsupportedOperationException.class,
        () -> events.add(new DonacionCargada(UUID.randomUUID(), UUID.randomUUID())));
  }

  @Test
  void clearDomainEvents_debeLimpiarEventosRegistrados() {
    assertEquals(1, donacion.getDomainEvents().size());

    donacion.clearDomainEvents();

    assertEquals(0, donacion.getDomainEvents().size());
  }
}
