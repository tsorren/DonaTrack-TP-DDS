package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemDonacionTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private Bien bien;

  @BeforeEach
  void setUp() {
    bien = new Bien("Abrigo de invierno", "abrigo.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
  }

  @Test
  void crearItemDonacion_conDatosValidos_deberiaSuceder() {
    ItemDonacion item = new ItemDonacion(bien, 10);

    assertEquals(bien, item.bien());
    assertEquals(10, item.cantidad());
  }

  @Test
  void crearItemDonacion_conBienNulo_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new ItemDonacion(null, 5),
        "Debería lanzar error cuando el bien es nulo");
  }

  @Test
  void crearItemDonacion_conCantidadNula_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new ItemDonacion(bien, null),
        "Debería lanzar error cuando la cantidad es nula");
  }

  @Test
  void crearItemDonacion_conCantidadCero_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new ItemDonacion(bien, 0),
        "Debería lanzar error cuando la cantidad es cero");
  }

  @Test
  void crearItemDonacion_conCantidadNegativa_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new ItemDonacion(bien, -5),
        "Debería lanzar error cuando la cantidad es negativa");
  }

  @Test
  void crearItemDonacion_conCantidadPositiva_deberiaSuceder() {
    ItemDonacion item1 = new ItemDonacion(bien, 1);
    ItemDonacion item2 = new ItemDonacion(bien, 100);

    assertEquals(1, item1.cantidad());
    assertEquals(100, item2.cantidad());
  }
}
