package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.models.entities.bienes.*;
import grupo5.donaciones.models.entities.donaciones.DonacionIndependiente;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubCategoriaTest {

  private SubCategoria subcategoria;
  private Bien bien;

  @BeforeEach
  void setUp() {

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);

    subcategoria = new SubCategoria(categoria, "Ropa de Invierno");

    bien =
        new Bien(
            "descripcion", "imagen.png", LocalDate.now().plusMonths(2), Estado.NUEVO, subcategoria);
  }

  @Test
  void calcularStock_SumaCorrectamenteLosItemsDeLasDonaciones() {
    ItemDonacion item1 = new ItemDonacion(bien, 5);

    ItemDonacion item2 = new ItemDonacion(bien, 10);

    ItemDonacion item3 = new ItemDonacion(bien, 5);

    DonacionIndependiente donacion1 = new DonacionIndependiente("Donación de camperas");
    donacion1.agregarItem(item1);
    donacion1.agregarItem(item2);

    DonacionIndependiente donacion2 = new DonacionIndependiente("Donación de bufanda");
    donacion2.agregarItem(item3);

    subcategoria.agregarDonacion(donacion1);
    subcategoria.agregarDonacion(donacion2);

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

  @Test
  void agregarDonacion_noDeberiaPermitirNulos() {
    assertThrows(
        IllegalArgumentException.class,
        () -> subcategoria.agregarDonacion(null),
        "Debería lanzar error al agregar una donación nula");
  }
}
