package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Las colecciones de los agregados solo cambian a través de sus métodos de comportamiento. */
class ColeccionesInmodificablesTest {

  @Test
  void gettersDeColecciones_cuandoSeIntentaModificar_lanzanUnsupportedOperation() {
    UUID subcategoriaId = UUID.randomUUID();
    DonacionIndependiente donacion =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 10);
    NecesidadRecurrente recurrente = NecesidadMother.recurrenteSemanal(subcategoriaId, 10);
    // El constructor de 2 argumentos arranca con un ArrayList mutable.
    Propuesta propuesta = new Propuesta();

    assertAll(
        () -> assertThrows(UnsupportedOperationException.class, donacion.getItems()::clear),
        // DTI-06: la necesidad y el período ya no exponen listas de donaciones
        () -> assertThrows(UnsupportedOperationException.class, recurrente.getPeriodos()::clear),
        () ->
            assertThrows(
                UnsupportedOperationException.class,
                propuesta.getPosiblesFragmentaciones()::clear));
  }
}
