package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadRecurrenteTest {

  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private NecesidadRecurrente necesidad;
  private DonacionIndependiente d40;
  private DonacionIndependiente d100;
  private UUID subcategoriaId;

  @BeforeEach
  void setUp() {
    subcategoriaId = UUID.randomUUID();
    necesidad =
        NecesidadMother.recurrenteConFecha(
            subcategoriaId, 100, TEST_DATE.minusDays(5), Period.ofWeeks(1));

    d40 = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 40);
    d100 = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 100);
  }

  @Test
  void estaSatisfecha_cuandoNoAlcanzaObjetivo_deberiaSerFalse() {
    necesidad.asignarDonacion(d40);

    assertFalse(necesidad.estaSatisfecha());
    assertEquals(40, necesidad.cantidadAcumulada());
  }

  @Test
  void estaSatisfecha_cuandoAlcanzaObjetivo_deberiaSerTrue() {
    necesidad.asignarDonacion(d100);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void generarNuevoPeriodo_deberiaComenzarConCantidadesEnCeroYGuardarElHistorico() {
    necesidad.asignarDonacion(d100);
    assertTrue(necesidad.estaSatisfecha());
    assertEquals(100, necesidad.cantidadAcumulada());

    necesidad.generarNuevoPeriodo();

    assertFalse(necesidad.estaSatisfecha());
    assertEquals(0, necesidad.cantidadAcumulada());
    assertEquals(2, necesidad.getPeriodos().size());
  }

  @Test
  void hayQueGenerarNuevo_cuandoPeriodoVencio_deberiaSerTrue() {
    NecesidadRecurrente necesidadVencida =
        NecesidadMother.recurrenteConFecha(
            subcategoriaId, 100, TEST_DATE.minusDays(10), Period.ofWeeks(1));

    assertTrue(necesidadVencida.hayQueGenerarNuevo(TEST_DATE));
  }

  @Test
  void hayQueGenerarNuevo_cuandoPeriodoAunEstaVigente_deberiaSerFalse() {
    assertFalse(necesidad.hayQueGenerarNuevo(TEST_DATE));
  }

  @Test
  void asignarDonacion_cuandoNoHayPeriodoActivo_deberiaLanzarExcepcion() {
    // Como ahora son inmutables desde afuera, estamos forzados a crear una subclase anonima solo
    // para esta prueba,
    // sobrescribiendo obtenerPeriodoActual para simular el caso
    NecesidadRecurrente necesidadSinPeriodos =
        new NecesidadRecurrente(
            subcategoriaId, 100, "Test sin periodos", Period.ofWeeks(1), TEST_DATE.minusDays(5)) {
          @Override
          public PeriodoNecesidad obtenerPeriodoActual() {
            return null;
          }
        };

    BusinessStateException excepcion =
        assertThrows(
            BusinessStateException.class, () -> necesidadSinPeriodos.asignarDonacion(d100));

    assertEquals(ErrorCatalog.SIN_PERIODO_ACTIVO, excepcion.getError());
  }

  @Test
  void renovarPeriodoSiCorresponde_cuandoPeriodoAunEstaVigente_deberiaRetornarFalse() {
    assertFalse(necesidad.renovarPeriodoSiCorresponde(TEST_DATE));
    assertEquals(1, necesidad.getPeriodos().size());
  }

  @Test
  void renovarPeriodoSiCorresponde_cuandoPeriodoVencio_deberiaRetornarTrueYCrearNuevoPeriodo() {
    LocalDate fechaFutura = TEST_DATE.plusDays(10);
    necesidad.asignarDonacion(d40);

    assertTrue(necesidad.renovarPeriodoSiCorresponde(fechaFutura));

    assertEquals(2, necesidad.getPeriodos().size());
    assertEquals(0, necesidad.cantidadAcumulada());
    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void renovarPeriodoSiCorresponde_cuandoNoTienePeriodos_deberiaRetornarTrueYCrearPeriodo() {
    // Instanciamos la entidad anulando temporalmente el período inicial
    NecesidadRecurrente necesidadSinPeriodos =
        new NecesidadRecurrente(
            subcategoriaId, 100, "Test sin periodos", Period.ofWeeks(1), TEST_DATE.minusDays(5)) {
          private boolean sinPeriodos = true;

          @Override
          public PeriodoNecesidad obtenerPeriodoActual() {
            return sinPeriodos ? null : super.obtenerPeriodoActual();
          }

          @Override
          public void generarNuevoPeriodo() {
            this.sinPeriodos = false;
            super.generarNuevoPeriodo();
          }
        };

    assertTrue(necesidadSinPeriodos.renovarPeriodoSiCorresponde(TEST_DATE));
    assertEquals(2, necesidadSinPeriodos.getPeriodos().size()); // Se sumó el nuevo período
  }
}
