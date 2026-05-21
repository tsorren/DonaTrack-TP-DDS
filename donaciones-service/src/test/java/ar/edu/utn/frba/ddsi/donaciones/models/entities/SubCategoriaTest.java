package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.SubCategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.DonacionIndependiente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ItemDonacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubCategoriaTest {

  private SubCategoria subcategoria;

  @BeforeEach
  void setUp() {
    subcategoria = new SubCategoria();
    subcategoria.setNombre("Ropa de Invierno");
  }

  @Test
  void calcularStock_SumaCorrectamenteLosItemsDeLasDonaciones() {
    ItemDonacion item1 = new ItemDonacion();
    item1.setCantidad(5);

    ItemDonacion item2 = new ItemDonacion();
    item2.setCantidad(10);

    ItemDonacion item3 = new ItemDonacion();
    item3.setCantidad(5);

    DonacionIndependiente donacion1 = new DonacionIndependiente();
    donacion1.setDescripcion("Donación de camperas");
    donacion1.agregarItem(item1);
    donacion1.agregarItem(item2);

    DonacionIndependiente donacion2 = new DonacionIndependiente();
    donacion2.setDescripcion("Donación de bufandas");
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
        () -> {
          subcategoria.agregarDonacion(null);
        },
        "Debería lanzar error al agregar una donación nula");
  }
}
