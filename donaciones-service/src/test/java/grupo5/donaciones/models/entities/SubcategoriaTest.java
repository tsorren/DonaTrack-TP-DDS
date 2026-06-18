package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubcategoriaTest {
  private Subcategoria subcategoria;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Electronica", false, true, Unidad.UNIDADES);
    subcategoria = new Subcategoria(categoria, "Celular");
  }

  @Test
  void constructor_conCategoriaNull_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new Subcategoria(null, "Ropa de Invierno"),
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
            () -> new Subcategoria(categoria, null),
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
            () -> new Subcategoria(categoria, "   "),
            "Debería lanzar excepción cuando el nombre está vacío");
    assertNotNull(exception);
    assertEquals(ErrorCatalog.SUBCATEGORIA_SIN_NOMBRE, exception.getError());
  }

  @Test
  void constructor_conParametrosValidos_debeCrearseCorrectamente() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    String nombre = "Ropa de Verano";
    Subcategoria nuevaSubcategoria = new Subcategoria(categoria, nombre);

    assertNotNull(nuevaSubcategoria);
    assertEquals(nombre, nuevaSubcategoria.getNombre());
    assertEquals(categoria, nuevaSubcategoria.getCategoria());
  }

  @Test
  void agregarAlias_debeAgregarseALaLista() {
    subcategoria.agregarAlias("celu");

    assertEquals(1, subcategoria.getAliases().size());
    assertEquals("celu", subcategoria.getAliases().get(0).getAlias());
  }

  @Test
  void tieneAlias_cuandoExiste_debeRetornarTrue() {
    subcategoria.agregarAlias("movil");

    assertTrue(subcategoria.tieneAlias("movil"));
  }

  @Test
  void tieneAlias_cuandoNoExiste_debeRetornarFalse() {
    subcategoria.agregarAlias("movil");

    assertFalse(subcategoria.tieneAlias("celu"));
  }

  @Test
  void removerAlias_debeEliminarloDeLaLista() {
    subcategoria.agregarAlias("celu");
    subcategoria.agregarAlias("movil");

    subcategoria.removerAlias("celu");

    assertFalse(subcategoria.tieneAlias("celu"));
    assertTrue(subcategoria.tieneAlias("movil"));
  }

  @Test
  void removerAlias_cuandoNoExiste_noDebeModificarLaLista() {
    subcategoria.agregarAlias("celu");

    subcategoria.removerAlias("telefono");

    assertEquals(1, subcategoria.getAliases().size());
  }
}
