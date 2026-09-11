package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
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

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMenor_deberiaSerFalse() {
    necesidad.asignarDonacion(primeraDonacionParcial);
    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaIgual_deberiaSerTrue() {
    necesidad.asignarDonacion(primeraDonacionParcial);
    necesidad.asignarDonacion(segundaDonacionParcial);
    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMayor_deberiaSerTrue() {
    necesidad.asignarDonacion(primeraDonacionParcial);
    necesidad.asignarDonacion(segundaDonacionParcial);
    necesidad.asignarDonacion(terceraDonacionParcial);
    assertTrue(necesidad.estaSatisfecha());
  }
}
