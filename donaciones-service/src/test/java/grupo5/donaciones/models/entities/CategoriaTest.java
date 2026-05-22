package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.models.entities.bienes.Categoria;
import grupo5.donaciones.models.entities.bienes.Unidad;
import org.junit.jupiter.api.Test;

class CategoriaTest {

  @Test
  void crearCategoria_conDatosValidos_deberiaSuceder() {
    Categoria categoria = new Categoria("Ropa", true, false, Unidad.UNIDADES);

    assertEquals("Ropa", categoria.getNombre());
    assertEquals(true, categoria.getConUso());
    assertEquals(false, categoria.getConVencimiento());
    assertEquals(Unidad.UNIDADES, categoria.getTipoUnidad());
  }

  @Test
  void crearCategoria_conNombreNulo_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Categoria(null, true, false, Unidad.UNIDADES),
        "Debería lanzar error cuando el nombre es nulo");
  }

  @Test
  void crearCategoria_conNombreVacio_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Categoria("   ", true, false, Unidad.UNIDADES),
        "Debería lanzar error cuando el nombre está vacío");
  }

  @Test
  void crearCategoria_conConUsoNulo_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Categoria("Ropa", null, false, Unidad.UNIDADES),
        "Debería lanzar error cuando conUso es nulo");
  }

  @Test
  void crearCategoria_conConVencimientoNulo_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Categoria("Ropa", true, null, Unidad.UNIDADES),
        "Debería lanzar error cuando conVencimiento es nulo");
  }

  @Test
  void crearCategoria_conUnidadNula_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Categoria("Ropa", true, false, null),
        "Debería lanzar error cuando la unidad es nula");
  }

  @Test
  void crearCategoria_conDiferentesUnidades() {
    Categoria categoriaKilogramo = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    Categoria categoriaLitros = new Categoria("Bebidas", false, true, Unidad.LITROS);

    assertEquals(Unidad.KILOGRAMO, categoriaKilogramo.getTipoUnidad());
    assertEquals(Unidad.LITROS, categoriaLitros.getTipoUnidad());
  }
}
