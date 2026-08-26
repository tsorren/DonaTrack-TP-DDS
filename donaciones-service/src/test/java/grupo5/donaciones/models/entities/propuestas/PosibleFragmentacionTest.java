package grupo5.donaciones.models.entities.propuestas;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PosibleFragmentacionTest {

  private DonacionIndependiente donacionOriginal;
  private NecesidadExtraordinaria necesidad;

  @BeforeEach
  void setUp() {
    UUID subcategoriaId = UUID.randomUUID();
    donacionOriginal = DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 10);
    necesidad = NecesidadMother.extraordinaria(subcategoriaId, 5);
  }

  @Test
  void confirmar_cuandoCantidadEsMenorQueDonacion_deberiaFragmentarYAsignar() {
    PosibleFragmentacion fragmentacion = new PosibleFragmentacion();
    fragmentacion.setDonacionOriginal(donacionOriginal);
    fragmentacion.setDonacionOriginalId(donacionOriginal.getId());
    fragmentacion.setCantidadNecesaria(4);

    DonacionIndependiente donacionAsignada = fragmentacion.confirmar(necesidad, "admin");

    assertNotNull(donacionAsignada);
    assertNotEquals(donacionOriginal.getId(), donacionAsignada.getId());
    assertEquals(4, donacionAsignada.getCantidad());
    assertEquals(6, donacionOriginal.getCantidad());
    assertInstanceOf(AsignacionRealizada.class, donacionAsignada.getEstadoActual());
    assertEquals(necesidad, donacionAsignada.getAsignadaA());
    assertEquals(4, necesidad.cantidadAcumulada());
  }

  @Test
  void confirmar_cuandoCantidadEsIgual_noDeberiaFragmentarYAsignarDirectamente() {
    PosibleFragmentacion fragmentacion = new PosibleFragmentacion();
    fragmentacion.setDonacionOriginal(donacionOriginal);
    fragmentacion.setDonacionOriginalId(donacionOriginal.getId());
    fragmentacion.setCantidadNecesaria(10);

    DonacionIndependiente donacionAsignada = fragmentacion.confirmar(necesidad, "admin");

    assertSame(donacionOriginal, donacionAsignada);
    assertEquals(10, donacionAsignada.getCantidad());
    assertInstanceOf(AsignacionRealizada.class, donacionAsignada.getEstadoActual());
    assertEquals(necesidad, donacionAsignada.getAsignadaA());
    assertEquals(10, necesidad.cantidadAcumulada());
  }

  @Test
  void confirmar_conParametrosNulosOInvalidos_deberiaLanzarValidacion() {
    PosibleFragmentacion fragmentacion = new PosibleFragmentacion();
    fragmentacion.setDonacionOriginal(donacionOriginal);
    fragmentacion.setCantidadNecesaria(5);

    assertThrows(ValidationException.class, () -> fragmentacion.confirmar(null, "admin"));

    PosibleFragmentacion fragmentacionSinDonacion = new PosibleFragmentacion();
    fragmentacionSinDonacion.setCantidadNecesaria(5);
    assertThrows(
        ValidationException.class, () -> fragmentacionSinDonacion.confirmar(necesidad, "admin"));

    PosibleFragmentacion fragmentacionCantidadInvalida = new PosibleFragmentacion();
    fragmentacionCantidadInvalida.setDonacionOriginal(donacionOriginal);
    fragmentacionCantidadInvalida.setCantidadNecesaria(0);
    assertThrows(
        ValidationException.class,
        () -> fragmentacionCantidadInvalida.confirmar(necesidad, "admin"));
  }
}
