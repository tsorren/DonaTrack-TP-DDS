package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadExtraordinariaTest {

  private NecesidadExtraordinaria necesidad;
  private DonacionIndependiente primeraDonacionParcial;
  private DonacionIndependiente segundaDonacionParcial;
  private DonacionIndependiente terceraDonacionParcial;

  @BeforeEach
  void setUp() {
    UUID subcategoriaId = UUID.randomUUID();
    necesidad = NecesidadMother.extraordinaria(subcategoriaId, 30);

    primeraDonacionParcial = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 15);
    segundaDonacionParcial = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 15);
    terceraDonacionParcial = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 15);
  }

  // DTI-06: la necesidad no guarda sus donaciones; lo acumulado es la suma de sus cantidades
  private static int acumulado(DonacionIndependiente... donaciones) {
    int total = 0;
    for (DonacionIndependiente d : donaciones) {
      total += d.getCantidad();
    }
    return total;
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMenor_deberiaSerFalse() {
    assertFalse(necesidad.estaSatisfechaCon(acumulado(primeraDonacionParcial)));
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaIgual_deberiaSerTrue() {
    assertTrue(
        necesidad.estaSatisfechaCon(acumulado(primeraDonacionParcial, segundaDonacionParcial)));
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMayor_deberiaSerTrue() {
    assertTrue(
        necesidad.estaSatisfechaCon(
            acumulado(primeraDonacionParcial, segundaDonacionParcial, terceraDonacionParcial)));
  }

  @Test
  void cantidadRestante_descuentaLoAsignadoSinBajarDeCero() {
    assertEquals(15, necesidad.cantidadRestante(acumulado(primeraDonacionParcial)));
    assertEquals(
        0,
        necesidad.cantidadRestante(
            acumulado(primeraDonacionParcial, segundaDonacionParcial, terceraDonacionParcial)));
  }

  @Test
  void destinoDeAsignacion_esLaNecesidadYSuEntidadSinPeriodo() {
    UUID entidadId = UUID.randomUUID();
    necesidad.asociarAEntidad(entidadId);

    DestinoAsignacion destino = necesidad.destinoDeAsignacion();

    assertEquals(necesidad.getId(), destino.necesidadId());
    assertNull(destino.periodoId());
    assertEquals(entidadId, destino.entidadBeneficiariaId());
    assertNull(necesidad.periodoActualId());
  }
}
