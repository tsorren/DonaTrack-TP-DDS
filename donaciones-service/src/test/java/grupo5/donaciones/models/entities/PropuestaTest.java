package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropuestaTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private NecesidadExtraordinaria necesidad;
  private DonacionIndependiente donacionConSobrante;
  private DonacionIndependiente donacionExacta;

  @BeforeEach
  void setUp() {
    Donacion donacionOriginal =
        new Donacion(new Donante(new Humana("nombre", "apellido", TEST_DATE)));
    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria, "Muebles Escolares");
    Bien bien = new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    BienNormalizado bienNormalizado =
        new BienNormalizado(bien, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);

    necesidad = new NecesidadExtraordinaria(subcategoria, 5, "bancos para el aula");

    List<ItemDonacionIndependiente> itemsDiez = new ArrayList<>();
    itemsDiez.add(new ItemDonacionIndependiente(bienNormalizado, 10));
    donacionConSobrante = new DonacionIndependiente(donacionOriginal, itemsDiez);

    List<ItemDonacionIndependiente> itemsCinco = new ArrayList<>();
    itemsCinco.add(new ItemDonacionIndependiente(bienNormalizado, 5));
    donacionExacta = new DonacionIndependiente(donacionOriginal, itemsCinco);
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
  void rechazar_debeSetearEstadoDescartado() {
    Propuesta propuesta = new Propuesta();
    propuesta.setEstado(EstadoPropuesta.PENDIENTE);

    propuesta.rechazar();

    assertEquals(EstadoPropuesta.DESCARTADA, propuesta.getEstado());
  }

  @Test
  void agregarFragmentacion_debeCrearUnaPosibleFragmentacion() {
    Propuesta propuesta = new Propuesta();

    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    assertEquals(1, propuesta.getPosiblesFragmentaciones().size());
  }

  @Test
  void confirmar_cuandoDonacionTieneMasCantidadDeLaNecesaria_debeFragmentarYAsignar() {
    Propuesta propuesta = new Propuesta();
    propuesta.setNecesidadQueSatisface(necesidad);
    propuesta.setEstado(EstadoPropuesta.PENDIENTE);
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    propuesta.confirmar();

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals(1, necesidad.getDonacionesAsignadas().size());
    assertEquals(5, necesidad.getDonacionesAsignadas().getFirst().getCantidad());
    assertEquals(5, donacionConSobrante.getCantidad());
  }

  @Test
  void confirmar_cuandoDonacionTieneLaCantidadExacta_debeUsarLaDonacionDirectamente() {
    Propuesta propuesta = new Propuesta();
    propuesta.setNecesidadQueSatisface(necesidad);
    propuesta.setEstado(EstadoPropuesta.PENDIENTE);
    propuesta.agregarFragmentacion(donacionExacta, 5);

    propuesta.confirmar();

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    assertEquals(1, necesidad.getDonacionesAsignadas().size());
    assertSame(donacionExacta, necesidad.getDonacionesAsignadas().getFirst());
    assertInstanceOf(AsignacionRealizada.class, donacionExacta.getEstadoActual());
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
  void confirmar_conNecesidadNula_debeLanzarExcepcion() {
    Propuesta propuesta = new Propuesta();
    propuesta.agregarFragmentacion(donacionConSobrante, 5);

    ValidationException exception = assertThrows(ValidationException.class, propuesta::confirmar);
    assertEquals(ErrorCatalog.PROPUESTA_CONFIRMAR_SIN_NECESIDAD, exception.getError());
  }
}
