package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.bienes.Bien;
import grupo5.donaciones.models.entities.bienes.Estado;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubCategoriaTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private SubCategoria subcategoria;
  private Bien bien;

  @BeforeEach
  void setUp() {

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);

    subcategoria = new SubCategoria(categoria, "Ropa de Invierno");

    bien =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO, subcategoria);
  }

  @Test
  void constructor_conCategoriaNull_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new SubCategoria(null, "Ropa de Invierno"),
            "Debería lanzar excepción cuando la categoría es nula");
    assertNotNull(exception);
    assertEquals(ErrorCatalog.SUBCATEGORIA_SIN_CATEGORIA, exception.getError());
  }

  @Test
  void constructor_conNombreNull_debeLanzarExcepcion() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new SubCategoria(categoria, null),
            "Debería lanzar excepción cuando el nombre es nulo");
    assertNotNull(exception);
    assertEquals(ErrorCatalog.SUBCATEGORIA_SIN_NOMBRE, exception.getError());
  }

  @Test
  void constructor_conNombreVacio_debeLanzarExcepcion() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new SubCategoria(categoria, "   "),
            "Debería lanzar excepción cuando el nombre está vacío");
    assertNotNull(exception);
    assertEquals(ErrorCatalog.SUBCATEGORIA_SIN_NOMBRE, exception.getError());
  }

  @Test
  void constructor_conParametrosValidos_debeCrearseCorrectamente() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    String nombre = "Ropa de Verano";
    SubCategoria nuevaSubCategoria = new SubCategoria(categoria, nombre);

    assertNotNull(nuevaSubCategoria);
    assertEquals(nombre, nuevaSubCategoria.getNombre());
    assertEquals(categoria, nuevaSubCategoria.getCategoria());
  }

  /* TODO: Pasar a capa service
  @Test
  void calcularStock_SumaCorrectamenteLosItemsDeLasDonaciones() {
    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bien, 5);

    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bien, 10);

    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bien, 5);

    List<ItemDonacionIndependiente> items1 = new ArrayList<>();
    items1.add(item1);
    items1.add(item2);
    new DonacionIndependiente(subcategoria, items1);

    List<ItemDonacionIndependiente> items2 = new ArrayList<>();
    items2.add(item3);

    new DonacionIndependiente(subcategoria, items2);

    Integer stockTotal = subcategoria.calcularStock();

    assertEquals(
        20,
        stockTotal,
        "El stock total debería ser la suma exacta de todos los ítems de las donaciones asociadas.");
  }

  @Test
  void calcularStock_cuandoNoHayDonaciones_deberiaRetornarCero() {
    assertEquals(0, subcategoria.calcularStock(), "El stock debe ser 0 si no hay donaciones");
  }
  */
}
