package grupo5.donaciones.models.entities.propuestas;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.events.EventoDeDominio;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropuestaTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private NecesidadExtraordinaria necesidad;
  private DonacionIndependiente donacionConSobrante;
  private DonacionIndependiente donacionExacta;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("nombre", "apellido", TEST_DATE);
    Donante donante = new Donante(humana);
    UUID donanteId = donante.getId();
    Donacion donacionOriginal = new Donacion(donanteId);
    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Muebles Escolares");
    Bien bien =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO, 1.0, 1.0);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, true);

    necesidad = new NecesidadExtraordinaria(subcategoria.getId(), 5, "bancos para el aula");

    List<ItemDonacionIndependiente> itemsDiez = new ArrayList<>();
    itemsDiez.add(new ItemDonacionIndependiente(bienNormalizado, 10));
    donacionConSobrante = new DonacionIndependiente(donacionOriginal.getId(), itemsDiez);

    List<ItemDonacionIndependiente> itemsCinco = new ArrayList<>();
    itemsCinco.add(new ItemDonacionIndependiente(bienNormalizado, 5));
    donacionExacta = new DonacionIndependiente(donacionOriginal.getId(), itemsCinco);
  }

  @Test
  void estaActiva_cuandoEstadoEsPendiente_debeSerTrue() {
    Propuesta propuesta = new Propuesta();
    propuesta.setEstado(EstadoPropuesta.PENDIENTE);

    assertTrue(propuesta.estaActiva());
  }

  @Test
  void estaActiva_cuandoEstadoEsAprobada_debeSerTrue() {
    Propuesta propuesta = new Propuesta();
    propuesta.setEstado(EstadoPropuesta.APROBADA);

    assertTrue(propuesta.estaActiva());
  }

  @Test
  void estaActiva_cuandoEstadoEsDescartada_debeSerFalse() {
    Propuesta propuesta = new Propuesta();
    propuesta.setEstado(EstadoPropuesta.DESCARTADA);

    assertFalse(propuesta.estaActiva());
  }

  @Test
  void rechazar_cuandoEstadoEsPendiente_debeSetearEstadoDescartado() {
    Propuesta propuesta = new Propuesta();
    propuesta.setEstado(EstadoPropuesta.PENDIENTE);

    propuesta.rechazar();

    assertEquals(EstadoPropuesta.DESCARTADA, propuesta.getEstado());
  }

  @Test
  void rechazar_cuandoYaEstaAprobada_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.aceptar("actor");

    BusinessStateException exception =
        assertThrows(BusinessStateException.class, propuesta::rechazar);
    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void rechazar_cuandoYaEstaDescartada_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();
    propuesta.rechazar();

    BusinessStateException exception =
        assertThrows(BusinessStateException.class, propuesta::rechazar);
    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void agregarFragmentacion_debeCrearUnaPosibleFragmentacion() {
    Propuesta propuesta = new Propuesta();

    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    assertEquals(1, propuesta.getPosiblesFragmentaciones().size());
  }

  @Test
  void aceptar_conActorExplicito_debeRegistrarActorEnEvento() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    propuesta.aceptar("admin-user");

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals(1, propuesta.getDomainEvents().size());
    PropuestaAprobada event = propuesta.getDomainEvents().getFirst();
    assertEquals("admin-user", event.getActor());
    assertEquals("admin-user", event.actor());
    assertInstanceOf(EventoDeDominio.class, event);
    assertNotNull(event.getId());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void aceptar_conActorNulo_debeAsignarSistemaComoActorPorDefecto() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    propuesta.aceptar(null);

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals("SISTEMA", propuesta.getDomainEvents().getFirst().getActor());
  }

  @Test
  void aceptar_conActorVacioOEnBlanco_debeAsignarSistemaComoActorPorDefecto() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    propuesta.aceptar("   ");

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals("SISTEMA", propuesta.getDomainEvents().getFirst().getActor());
  }

  @Test
  void aceptar_cuandoDonacionTieneMasCantidadDeLaNecesaria_debeFragmentarYAsignar() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    propuesta.aceptar("admin");

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals(1, propuesta.getDomainEvents().size());
    PropuestaAprobada event = propuesta.getDomainEvents().getFirst();

    // Simulate listener mutation
    String actor = event.actor();
    for (PosibleFragmentacion f : event.fragmentaciones()) {
      DonacionIndependiente donacionOriginal = donacionConSobrante;
      Integer cantidadNecesaria = f.getCantidadNecesaria();
      DonacionIndependiente donacionAsignar;

      if (donacionOriginal.getCantidad() > cantidadNecesaria) {
        donacionAsignar = donacionOriginal.fragmentarse(cantidadNecesaria);
      } else {
        donacionAsignar = donacionOriginal;
      }

      donacionAsignar.asignar(actor, necesidad);
      necesidad.asignarDonacion(donacionAsignar);
    }

    assertEquals(1, necesidad.getDonacionesAsignadas().size());
    assertEquals(5, necesidad.getDonacionesAsignadas().getFirst().getCantidad());
    assertEquals(5, donacionConSobrante.getCantidad());
  }

  @Test
  void aceptar_cuandoDonacionTieneLaCantidadExacta_debeUsarLaDonacionDirectamente() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(donacionExacta, 5);

    propuesta.aceptar("admin");

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals(1, propuesta.getDomainEvents().size());
    PropuestaAprobada event = propuesta.getDomainEvents().getFirst();

    // Simulate listener mutation
    String actor = event.actor();
    for (PosibleFragmentacion f : event.fragmentaciones()) {
      DonacionIndependiente donacionOriginal = donacionExacta;
      Integer cantidadNecesaria = f.getCantidadNecesaria();
      DonacionIndependiente donacionAsignar;

      if (donacionOriginal.getCantidad() > cantidadNecesaria) {
        donacionAsignar = donacionOriginal.fragmentarse(cantidadNecesaria);
      } else {
        donacionAsignar = donacionOriginal;
      }

      donacionAsignar.asignar(actor, necesidad);
      necesidad.asignarDonacion(donacionAsignar);
    }

    assertEquals(1, necesidad.getDonacionesAsignadas().size());
    assertSame(donacionExacta, necesidad.getDonacionesAsignadas().getFirst());
    assertInstanceOf(AsignacionRealizada.class, donacionExacta.getEstadoActual());
  }

  @Test
  void aceptar_cuandoYaEstaAprobada_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.aceptar("actor");

    BusinessStateException exception =
        assertThrows(BusinessStateException.class, () -> propuesta.aceptar("otro-actor"));
    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void aceptar_cuandoYaEstaDescartada_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.rechazar();

    BusinessStateException exception =
        assertThrows(BusinessStateException.class, () -> propuesta.aceptar("actor"));
    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void aceptar_conNecesidadNula_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> propuesta.aceptar("actor"));
    assertEquals(ErrorCatalog.PROPUESTA_CONFIRMAR_SIN_NECESIDAD, exception.getError());
  }

  @Test
  void agregarFragmentacion_conDonacionNula_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();

    ValidationException exception =
        assertThrows(ValidationException.class, () -> propuesta.agregarFragmentacion(null, 5));
    assertEquals(ErrorCatalog.PROPUESTA_FRAGMENTACION_DONACION_NULA, exception.getError());
  }

  @Test
  void agregarFragmentacion_conCantidadCero_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();

    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> propuesta.agregarFragmentacion(donacionConSobrante, 0));
    assertEquals(ErrorCatalog.PROPUESTA_FRAGMENTACION_CANTIDAD_INVALIDA, exception.getError());
  }

  @Test
  void agregarFragmentacion_conCantidadNegativa_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();

    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> propuesta.agregarFragmentacion(donacionConSobrante, -3));
    assertEquals(ErrorCatalog.PROPUESTA_FRAGMENTACION_CANTIDAD_INVALIDA, exception.getError());
  }

  @Test
  void getDomainEvents_debeRetornarListaInmodificable() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.aceptar("actor");

    List<PropuestaAprobada> events = propuesta.getDomainEvents();
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            events.add(
                new PropuestaAprobada(UUID.randomUUID(), UUID.randomUUID(), List.of(), "hacker")));
  }

  @Test
  void clearDomainEvents_debeLimpiarLaListaDeEventos() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.aceptar("actor");

    assertEquals(1, propuesta.getDomainEvents().size());

    propuesta.clearDomainEvents();

    assertEquals(0, propuesta.getDomainEvents().size());
  }
}
