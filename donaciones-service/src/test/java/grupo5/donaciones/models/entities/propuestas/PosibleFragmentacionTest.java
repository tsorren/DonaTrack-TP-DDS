package grupo5.donaciones.models.entities.propuestas;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PosibleFragmentacionTest {

  private DonacionIndependiente donacionOriginal;
  private NecesidadExtraordinaria necesidad;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    Donacion donacion = new Donacion(donante.getId());

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Ropa de Invierno");
    Bien bien =
        new Bien("Abrigo", "abrigo.png", LocalDate.of(2027, Month.JUNE, 1), Estado.NUEVO, 1.0, 1.0);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacion.getId(), bienNormalizado, 10);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(itemNormalizado.getBien(), 10);

    donacionOriginal = new DonacionIndependiente(donacion.getId(), List.of(item));
    necesidad = new NecesidadExtraordinaria(subcategoria.getId(), 5, "Abrigos para comedor");
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
