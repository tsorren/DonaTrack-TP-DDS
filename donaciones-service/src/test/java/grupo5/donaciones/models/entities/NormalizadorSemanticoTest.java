package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemantico;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalizadorSemanticoTest {
  private Subcategoria subcategoriaElectronica;
  private NormalizadorSemantico normalizador;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Electronica", false, true, Unidad.UNIDADES);
    subcategoriaElectronica = new Subcategoria(categoria, "celular");
    subcategoriaElectronica.agregarAlias("celu");
    subcategoriaElectronica.agregarAlias("movil");
    subcategoriaElectronica.agregarAlias("telefono");

    normalizador = new NormalizadorSemantico(subcategoriaElectronica.getAliases());
  }

  @Test
  void normalizar_cuandoTextoTieneAlias_debeReemplazarloConNombreCanónico() {
    String resultado = normalizador.normalizar("necesito un celu");

    assertEquals("necesito un celular", resultado);
  }

  @Test
  void normalizar_cuandoTextoTieneOtroAlias_debeReemplazarloIgual() {
    String resultado = normalizador.normalizar("tengo un movil para donar");

    assertEquals("tengo un celular para donar", resultado);
  }

  @Test
  void normalizar_cuandoTextoNoTieneAlias_debeRetornarloSinCambios() {
    String resultado = normalizador.normalizar("necesito ropa de invierno");

    assertEquals("necesito ropa de invierno", resultado);
  }

  @Test
  void normalizar_cuandoTextoEsNulo_debeRetornarNulo() {
    assertNull(normalizador.normalizar(null));
  }

  @Test
  void normalizar_cuandoSubcategoriaNoTieneAliases_debeRetornarTextoNormalizado() {
    normalizador = new NormalizadorSemantico(new ArrayList<>());

    String resultado = normalizador.normalizar("necesito un celu");

    assertEquals("necesito un celu", resultado);
  }

  @Test
  void normalizar_conMultiplesSubcategorias_debeReemplazarTodosLosAliases() {
    Categoria categoria = new Categoria("Tecnologia", false, true, Unidad.UNIDADES);
    Subcategoria subcategoriaCompu = new Subcategoria(categoria, "computadora");
    subcategoriaCompu.agregarAlias("compu");
    subcategoriaCompu.agregarAlias("pc");

    List<grupo5.donaciones.models.entities.categorias.AliasSubcategoria> todosLosAliases =
        new ArrayList<>();
    todosLosAliases.addAll(subcategoriaElectronica.getAliases());
    todosLosAliases.addAll(subcategoriaCompu.getAliases());
    normalizador = new NormalizadorSemantico(todosLosAliases);

    String resultado = normalizador.normalizar("tengo una compu y un celu");

    assertEquals("tengo una computadora y un celular", resultado);
  }
}
