package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.models.entities.bienes.*;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemDonacionTest {

  private Bien bien;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    SubCategoria subcategoria = new SubCategoria(categoria, "Ropa de Invierno");

    bien =
        new Bien(
            "Abrigo de invierno",
            "abrigo.png",
            LocalDate.now().plusMonths(2),
            Estado.NUEVO,
            subcategoria);
  }

  @Test
  void crearItemDonacion_conDatosValidos_deberiaSuceder() {
    ItemDonacion item = new ItemDonacion(bien, 10);

    assertEquals(bien, item.getBien());
    assertEquals(10, item.getCantidad());
  }

  @Test
  void crearItemDonacion_conBienNulo_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ItemDonacion(null, 5),
        "Debería lanzar error cuando el bien es nulo");
  }

  @Test
  void crearItemDonacion_conCantidadNula_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ItemDonacion(bien, null),
        "Debería lanzar error cuando la cantidad es nula");
  }

  @Test
  void crearItemDonacion_conCantidadCero_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ItemDonacion(bien, 0),
        "Debería lanzar error cuando la cantidad es cero");
  }

  @Test
  void crearItemDonacion_conCantidadNegativa_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ItemDonacion(bien, -5),
        "Debería lanzar error cuando la cantidad es negativa");
  }

  @Test
  void crearItemDonacion_conCantidadPositiva_deberiaSuceder() {
    ItemDonacion item1 = new ItemDonacion(bien, 1);
    ItemDonacion item2 = new ItemDonacion(bien, 100);

    assertEquals(1, item1.getCantidad());
    assertEquals(100, item2.getCantidad());
  }
}
