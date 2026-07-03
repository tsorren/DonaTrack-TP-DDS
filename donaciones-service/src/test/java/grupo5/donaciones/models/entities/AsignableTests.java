package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
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
  private PeriodoNecesidad periodoNecesidad;
  private DonacionIndependiente donacionIndependiente;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("nombre", "apellido", TEST_DATE);
    Donante donante = new Donante(humana.getId());
    Donacion donacionOriginal = new Donacion(donante.getId());
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "No Perecederos");

    necesidadExtraordinaria =
        new NecesidadExtraordinaria(subcategoria.getId(), 100, "Latas de atún para comedor");

    necesidadRecurrente =
        new NecesidadRecurrente(
            subcategoria.getId(),
            50,
            "Leche en polvo",
            Period.ofMonths(1),
            TEST_DATE.minusDays(15));

    periodoNecesidad = necesidadRecurrente.obtenerPeriodoActual();

    Bien bienOriginal =
        new Bien("Arroz", "imagen.png", TEST_DATE.plusYears(1), Estado.NUEVO, 1.0, 1.0);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bienOriginal, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 10);
    donacionIndependiente = new DonacionIndependiente(donacionOriginal.getId(), List.of(item));
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
