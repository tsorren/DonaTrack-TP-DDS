package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.models.entities.bienes.Bien;
import grupo5.donaciones.models.entities.bienes.Estado;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsignableTests {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private NecesidadExtraordinaria necesidadExtraordinaria;
  private NecesidadRecurrente necesidadRecurrente;
  private DonacionIndependiente donacionIndependiente;

  @BeforeEach
  void setUp() {
    Donacion donacionOriginal =
        new Donacion(new Donante(new Humana("nombre", "apellido", TEST_DATE)));
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    SubCategoria subcategoria = new SubCategoria(categoria, "No Perecederos");

    necesidadExtraordinaria =
        new NecesidadExtraordinaria(subcategoria, 100, "Latas de atún para comedor");

    necesidadRecurrente =
        new NecesidadRecurrente(
            subcategoria, 50, "Leche en polvo", Period.ofMonths(1), TEST_DATE.minusDays(15));

    Bien bien = new Bien("Arroz", "imagen.png", TEST_DATE.plusYears(1), Estado.NUEVO, subcategoria);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bien, 10);
    donacionIndependiente =
        new DonacionIndependiente(donacionOriginal, subcategoria, List.of(item));
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
  void obtenerNecesidad_desdeNecesidadRecurrente_devuelveLaMismaInstancia() {
    Necesidad resultado = necesidadRecurrente.obtenerNecesidad();
    assertEquals(
        necesidadRecurrente,
        resultado,
        "Debería devolver la propia instancia de NecesidadRecurrente.");
  }

  @Test
  void obtenerNecesidad_desdeDonacionAsignadaAExtraordinaria_devuelveLaNecesidadCorrecta() {
    donacionIndependiente.setAsignadaA(necesidadExtraordinaria);
    Necesidad resultado = donacionIndependiente.asignadaA().obtenerNecesidad();
    assertEquals(
        necesidadExtraordinaria, resultado, "Debería devolver la necesidad a la que fue asignada.");
  }

  @Test
  void obtenerNecesidad_desdeDonacionAsignadaARecurrente_devuelveLaNecesidadCorrecta() {
    donacionIndependiente.setAsignadaA(necesidadRecurrente);
    Necesidad resultado = donacionIndependiente.asignadaA().obtenerNecesidad();
    assertEquals(
        necesidadRecurrente, resultado, "Debería devolver la necesidad a la que fue asignada.");
  }

  @Test
  void obtenerNecesidad_desdeDonacionNoAsignada_asignadaAEsNull() {
    assertNull(
        donacionIndependiente.asignadaA(),
        "Una donación no asignada debería tener asignadaA en null.");
  }
}
