package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsignableTest {

  private NecesidadExtraordinaria necesidadExtraordinaria;
  private NecesidadRecurrente necesidadRecurrente;
  private PeriodoNecesidad periodoNecesidad;
  private DonacionIndependiente donacionIndependiente;

  @BeforeEach
  void setUp() {
    UUID subcategoriaId = UUID.randomUUID();
    necesidadExtraordinaria = NecesidadMother.extraordinaria(subcategoriaId, 100);
    necesidadRecurrente = NecesidadMother.recurrenteSemanal(subcategoriaId, 50);
    periodoNecesidad = necesidadRecurrente.obtenerPeriodoActual();
    donacionIndependiente = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 10);
  }

  @Test
  void obtenerNecesidad_desdeNecesidadExtraordinaria_devuelveLaMismaInstancia() {
    Necesidad resultado = necesidadExtraordinaria.obtenerNecesidad();
    assertEquals(
        necesidadExtraordinaria,
        resultado,
        "Debería devolver la propia instancia de NecesidadExtraordinaria.");
  }

  @Test
  void obtenerNecesidad_desdeDonacionAsignadaAExtraordinaria_devuelveLaNecesidadCorrecta() {
    donacionIndependiente.asignar("SISTEMA", necesidadExtraordinaria);
    Necesidad resultado = donacionIndependiente.asignadaA().obtenerNecesidad();
    assertEquals(
        necesidadExtraordinaria, resultado, "Debería devolver la necesidad a la que fue asignada.");
  }

  @Test
  void obtenerNecesidad_desdeDonacionNoAsignada_asignadaAEsNull() {
    assertNull(
        donacionIndependiente.asignadaA(),
        "Una donación no asignada debería tener asignadaA en null.");
  }

  @Test
  void obtenerNecesidad_desdePeriodoNecesidad_devuelveLaNecesidadRecurrentePadre() {
    Necesidad resultado = periodoNecesidad.obtenerNecesidad();
    assertEquals(
        necesidadRecurrente,
        resultado,
        "El período debería delegar y devolver la NecesidadRecurrente asociada.");
  }

  @Test
  void obtenerNecesidad_desdeDonacionAsignadaAPeriodoRecurrente_devuelveLaNecesidadCorrecta() {
    donacionIndependiente.asignar("SISTEMA", periodoNecesidad);
    Necesidad resultado = donacionIndependiente.asignadaA().obtenerNecesidad();
    assertEquals(
        necesidadRecurrente,
        resultado,
        "Al pedirle la necesidad al asignable (período), debería llegar hasta la NecesidadRecurrente.");
  }
}
