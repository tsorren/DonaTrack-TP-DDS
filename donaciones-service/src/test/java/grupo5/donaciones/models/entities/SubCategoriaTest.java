package grupo5.donaciones.models.entities;

import grupo5.donaciones.models.entities.bienes.Bien;
import grupo5.donaciones.models.entities.bienes.Estado;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;

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
