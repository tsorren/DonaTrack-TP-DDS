package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.ZoneId;
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
    // DTI-06: lo acumulado del período lo suma quien llama, sobre las donaciones asignadas
    assertFalse(necesidad.estaSatisfechaCon(d40.getCantidad()));
    assertEquals(60, necesidad.cantidadRestante(d40.getCantidad()));
  }

  @Test
  void estaSatisfecha_cuandoAlcanzaObjetivo_deberiaSerTrue() {
    assertTrue(necesidad.estaSatisfechaCon(d100.getCantidad()));
  }

  @Test
  void destinoDeAsignacion_incluyeElPeriodoActual() {
    UUID entidadId = UUID.randomUUID();
    necesidad.asociarAEntidad(entidadId);

    DestinoAsignacion destino = necesidad.destinoDeAsignacion();

    assertEquals(necesidad.getId(), destino.necesidadId());
    assertEquals(necesidad.obtenerPeriodoActual().id(), destino.periodoId());
    assertEquals(necesidad.periodoActualId(), destino.periodoId());
    assertEquals(entidadId, destino.entidadBeneficiariaId());
  }

  @Test
  void generarNuevoPeriodo_deberiaComenzarConCantidadesEnCeroYGuardarElHistorico() {
    UUID periodoAnterior = necesidad.periodoActualId();
    assertTrue(necesidad.estaSatisfechaCon(d100.getCantidad()));

    necesidad.generarNuevoPeriodo();

    // Las donaciones quedaron con el id del período anterior: el nuevo arranca en cero
    assertNotEquals(periodoAnterior, necesidad.periodoActualId());
    assertFalse(necesidad.estaSatisfechaCon(0));
    assertEquals(2, necesidad.getPeriodos().size());
    assertEquals(periodoAnterior, necesidad.getPeriodos().getFirst().id());
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
    NecesidadRecurrente sinPeriodo = necesidadSinPeriodoActual();

    BusinessStateException excepcion =
        assertThrows(BusinessStateException.class, sinPeriodo::destinoDeAsignacion);

    assertEquals(ErrorCatalog.SIN_PERIODO_ACTIVO, excepcion.getError());
  }

  @Test
  void renovarPeriodoSiCorresponde_cuandoPeriodoAunEstaVigente_deberiaRetornarFalse() {
    assertFalse(necesidad.renovarPeriodoSiCorresponde(TEST_DATE, 0));
    assertEquals(1, necesidad.getPeriodos().size());
  }

  @Test
  void renovarPeriodoSiCorresponde_cuandoPeriodoVencio_deberiaRetornarTrueYCrearNuevoPeriodo() {
    LocalDate fechaFutura = TEST_DATE.plusDays(10);
    UUID periodoAnterior = necesidad.periodoActualId();

    assertTrue(necesidad.renovarPeriodoSiCorresponde(fechaFutura, d40.getCantidad()));

    assertEquals(2, necesidad.getPeriodos().size());
    assertNotEquals(periodoAnterior, necesidad.periodoActualId());
    assertFalse(necesidad.estaSatisfechaCon(0));
  }

  @Test
  void renovarPeriodoSiCorresponde_cuandoNoTienePeriodos_deberiaRetornarTrueYCrearPeriodo() {
    NecesidadRecurrente sinPeriodo = necesidadSinPeriodoActual();

    LocalDate antes = LocalDate.now(ZoneId.systemDefault());
    assertTrue(sinPeriodo.renovarPeriodoSiCorresponde(TEST_DATE, 0));
    LocalDate despues = LocalDate.now(ZoneId.systemDefault());

    // El período inicial sigue en la lista (solo está oculto); el nuevo parte de hoy.
    assertEquals(2, sinPeriodo.getPeriodos().size());
    LocalDate fechaFin = sinPeriodo.getPeriodos().getLast().fechaFin();
    assertFalse(fechaFin.isBefore(antes.plus(Period.ofWeeks(1))));
    assertFalse(fechaFin.isAfter(despues.plus(Period.ofWeeks(1))));
  }

  /** Los períodos no se pueden vaciar desde afuera: se simula que no hay período actual. */
  private NecesidadRecurrente necesidadSinPeriodoActual() {
    return new NecesidadRecurrente(
        subcategoriaId, 100, "Sin período actual", Period.ofWeeks(1), TEST_DATE.minusDays(5)) {
      @Override
      public PeriodoNecesidad obtenerPeriodoActual() {
        return null;
      }
    };
  }
}
