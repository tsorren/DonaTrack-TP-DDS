package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.bienes.Bien;
import grupo5.donaciones.models.entities.bienes.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemDonacionIndependienteFragmentacionTest {

  private Bien bien;
  private ItemDonacionIndependiente itemDonacion;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    SubCategoria subcategoria = new SubCategoria(categoria, "Ropa de Invierno");

    bien =
        new Bien(
            "descripcion", "imagen.png", LocalDate.now().plusMonths(2), Estado.NUEVO, subcategoria);

    itemDonacion = new ItemDonacionIndependiente(bien, 20);
  }

  @Test
  void fragmentarse_conCantidadMayorOMenor_debeLanzarExcepcion() {
    // Cantidad total es 20
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> itemDonacion.fragmentarse(20),
            "Debería lanzar excepción cuando la cantidad es igual o mayor");
    assertNotNull(exception);
  }

  @Test
  void fragmentarse_conCantidadMayorQueTotal_debeLanzarExcepcion() {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> itemDonacion.fragmentarse(25),
            "Debería lanzar excepción cuando solicita más de lo disponible");
    assertNotNull(exception);
  }

  @Test
  void fragmentarse_exitoso_disminuyeLaCantidadOriginal() {
    // Cantidad inicial 20, fragmentando 7
    itemDonacion.fragmentarse(7);

    assertEquals(13, itemDonacion.getCantidad(), "Debe disminuir en 7");
  }

  @Test
  void fragmentarse_exitoso_creaUnNuevoItem() {
    // Cantidad inicial 20, fragmentando 8
    ItemDonacionIndependiente itemFragmentado = itemDonacion.fragmentarse(8);

    assertNotNull(itemFragmentado, "Debe retornar un item fragmentado");
    assertEquals(8, itemFragmentado.getCantidad(), "El nuevo item debe tener cantidad 8");
    assertEquals(12, itemDonacion.getCantidad(), "El item original debe reducirse a 12");
  }

  @Test
  void fragmentarse_itemFragmentadoTieneCantidadCorrecta() {
    // Cantidad inicial 20, fragmentando 5
    ItemDonacionIndependiente itemFragmentado = itemDonacion.fragmentarse(5);

    assertNotNull(itemFragmentado, "Debe haber un nuevo item fragmentado");
    assertEquals(5, itemFragmentado.getCantidad(), "El nuevo item debe tener cantidad 5");
    assertEquals(15, itemDonacion.getCantidad(), "El original debe tener cantidad 15");
  }

  @Test
  void fragmentarse_nuevoItemBasta_BienDelOriginal() {
    ItemDonacionIndependiente itemFragmentado = itemDonacion.fragmentarse(5);

    assertNotNull(itemFragmentado);
    assertEquals(bien, itemFragmentado.getBien(), "El nuevo item debe tener el mismo bien");
  }

  @Test
  void fragmentarse_preservaCantidadTotal() {
    // El item original tiene 20
    ItemDonacionIndependiente itemFragmentado = itemDonacion.fragmentarse(6);

    // El item original debe tener 14, y el fragmentado 6
    assertEquals(14, itemDonacion.getCantidad(), "El item original debe tener 14");
    assertEquals(6, itemFragmentado.getCantidad(), "El fragmentado debe tener 6");
    assertEquals(20, 14 + 6, "La suma debe ser 20");
  }

  @Test
  void validarItemDonacion_conBienNulo_debeLanzarExcepcion() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemDonacionIndependiente(null, 10),
            "Debería lanzar excepción cuando el bien es nulo");
    assertEquals(
        "El item de donación debe tener un bien asociado.",
        exception.getMessage(),
        "Debe tener el mensaje correcto");
  }

  @Test
  void validarItemDonacion_conCantidadNula_debeLanzarExcepcion() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemDonacionIndependiente(bien, null),
            "Debería lanzar excepción cuando la cantidad es nula");
    assertEquals(
        "La cantidad del item debe ser mayor a cero.",
        exception.getMessage(),
        "Debe tener el mensaje correcto");
  }

  @Test
  void validarItemDonacion_conCantidadCero_debeLanzarExcepcion() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemDonacionIndependiente(bien, 0),
            "Debería lanzar excepción cuando la cantidad es cero");
    assertEquals(
        "La cantidad del item debe ser mayor a cero.",
        exception.getMessage(),
        "Debe tener el mensaje correcto");
  }

  @Test
  void validarItemDonacion_conCantidadNegativa_debeLanzarExcepcion() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemDonacionIndependiente(bien, -5),
            "Debería lanzar excepción cuando la cantidad es negativa");
    assertEquals(
        "La cantidad del item debe ser mayor a cero.",
        exception.getMessage(),
        "Debe tener el mensaje correcto");
  }

  @Test
  void fragmentarse_multipleFragmentaciones_preservaIntegridad() {
    // Inicio: 20
    // Primera fragmentación: 5 -> itemDonacion tiene 15, nuevo tiene 5
    ItemDonacionIndependiente item1 = itemDonacion.fragmentarse(5);
    assertEquals(15, itemDonacion.getCantidad());
    assertEquals(5, item1.getCantidad());

    // Segunda fragmentación: 7 -> itemDonacion tiene 8, nuevo tiene 7
    ItemDonacionIndependiente item2 = itemDonacion.fragmentarse(7);
    assertEquals(8, itemDonacion.getCantidad());
    assertEquals(7, item2.getCantidad());

    // Total debe ser 8 + 7 + 5 = 20
    int cantidadTotal = itemDonacion.getCantidad() + item1.getCantidad() + item2.getCantidad();
    assertEquals(20, cantidadTotal, "La cantidad total debe seguir siendo 20");
  }
}
