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
  private DonacionIndependiente donacion15_1;
  private DonacionIndependiente donacion15_2;
  private DonacionIndependiente donacion15_3;

  @BeforeEach
  void setUp() {
    UUID subcategoriaId = UUID.randomUUID();
    necesidad = NecesidadMother.extraordinaria(subcategoriaId, 30);

    donacion15_1 = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 15);
    donacion15_2 = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 15);
    donacion15_3 = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 15);
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMenor_deberiaSerFalse() {
    necesidad.asignarDonacion(donacion15_1);
    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaIgual_deberiaSerTrue() {
    necesidad.asignarDonacion(donacion15_1);
    necesidad.asignarDonacion(donacion15_2);
    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMayor_deberiaSerTrue() {
    necesidad.asignarDonacion(donacion15_1);
    necesidad.asignarDonacion(donacion15_2);
    necesidad.asignarDonacion(donacion15_3);
    assertTrue(necesidad.estaSatisfecha());
  }
}
