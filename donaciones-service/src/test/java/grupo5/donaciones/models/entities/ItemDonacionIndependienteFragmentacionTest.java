package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemDonacionIndependienteFragmentacionTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private BienNormalizado bienNormalizado;
  private ItemDonacionIndependiente itemDonacion;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Ropa de Invierno");

    Bien bienOriginal =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    bienNormalizado =
        new BienNormalizado(
            bienOriginal, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    itemDonacion = new ItemDonacionIndependiente(bienNormalizado, 20);
  }

  @Test
  void fragmentarse_conCantidadMayorOMenor_debeLanzarExcepcion() {
    // Cantidad total es 20
    BusinessStateException exception =
        assertThrows(
            BusinessStateException.class,
            () -> itemDonacion.fragmentarse(20),
            "Debería lanzar excepción cuando la cantidad es igual o mayor");
    assertNotNull(exception);
    assertEquals(
        ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_FRAGMENTACION_INVALIDA, exception.getError());
  }

  @Test
  void fragmentarse_conCantidadMayorQueTotal_debeLanzarExcepcion() {
    BusinessStateException exception =
        assertThrows(
            BusinessStateException.class,
            () -> itemDonacion.fragmentarse(25),
            "Debería lanzar excepción cuando solicita más de lo disponible");
    assertNotNull(exception);
    assertEquals(
        ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_FRAGMENTACION_INVALIDA, exception.getError());
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
    assertEquals(
        bienNormalizado, itemFragmentado.getBien(), "El nuevo item debe tener el mismo bien");
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
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new ItemDonacionIndependiente(null, 10),
            "Debería lanzar excepción cuando el bien es nulo");
    assertEquals(
        ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_SIN_BIEN,
        exception.getError(),
        "Debe tener el error correcto");
  }

  @Test
  void validarItemDonacion_conCantidadNula_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new ItemDonacionIndependiente(bienNormalizado, null),
            "Debería lanzar excepción cuando la cantidad es nula");
    assertEquals(
        ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_CANTIDAD_INVALIDA,
        exception.getError(),
        "Debe tener el error correcto");
  }

  @Test
  void validarItemDonacion_conCantidadCero_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new ItemDonacionIndependiente(bienNormalizado, 0),
            "Debería lanzar excepción cuando la cantidad es cero");
    assertEquals(
        ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_CANTIDAD_INVALIDA,
        exception.getError(),
        "Debe tener el error correcto");
  }

  @Test
  void validarItemDonacion_conCantidadNegativa_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new ItemDonacionIndependiente(bienNormalizado, -5),
            "Debería lanzar excepción cuando la cantidad es negativa");
    assertEquals(
        ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_CANTIDAD_INVALIDA,
        exception.getError(),
        "Debe tener el error correcto");
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
