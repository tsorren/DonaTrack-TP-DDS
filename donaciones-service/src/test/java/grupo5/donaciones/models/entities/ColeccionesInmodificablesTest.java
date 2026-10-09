package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Las colecciones de los agregados solo cambian a través de sus métodos de comportamiento. */
class ColeccionesInmodificablesTest {

  @Test
  void gettersDeColecciones_cuandoSeIntentaModificar_lanzanUnsupportedOperation() {
    UUID subcategoriaId = UUID.randomUUID();
    DonacionIndependiente donacion =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 10);
    NecesidadExtraordinaria extraordinaria = NecesidadMother.extraordinaria(subcategoriaId, 10);
    NecesidadRecurrente recurrente = NecesidadMother.recurrenteSemanal(subcategoriaId, 10);
    // El constructor de 2 argumentos arranca con un ArrayList mutable.
    PeriodoNecesidad periodo = new PeriodoNecesidad(LocalDate.of(2026, Month.JUNE, 30), 10);
    Propuesta propuesta = new Propuesta();

    assertAll(
        () -> assertThrows(UnsupportedOperationException.class, donacion.getItems()::clear),
        () ->
            assertThrows(
                UnsupportedOperationException.class,
                extraordinaria.getDonacionesAsignadas()::clear),
        () -> assertThrows(UnsupportedOperationException.class, recurrente.getPeriodos()::clear),
        () ->
            assertThrows(UnsupportedOperationException.class, periodo.donacionesAsignadas()::clear),
        () ->
            assertThrows(
                UnsupportedOperationException.class,
                propuesta.getPosiblesFragmentaciones()::clear));
  }
}
