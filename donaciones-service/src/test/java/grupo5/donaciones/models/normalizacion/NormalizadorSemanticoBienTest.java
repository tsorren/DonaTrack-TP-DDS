package grupo5.donaciones.models.normalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalizadorSemanticoBienTest {

  private ComparadorTexto comparador;
  private List<Subcategoria> subcategorias;
  private Map<UUID, Categoria> categoriasPorId;
  private Categoria categoria;
  private Donacion donacion;
  private NormalizadorSemanticoBien normalizador;

  @BeforeEach
  void setUp() {
    comparador = new ComparadorTexto(new NormalizadorBasicoTexto());
    subcategorias = new ArrayList<>();
    categoria = new Categoria("Varios", false, false, Unidad.UNIDADES);
    categoriasPorId = Map.of(categoria.getId(), categoria);

    Humana humana = new Humana("Juan", "Perez", LocalDate.of(2026, Month.JUNE, 18));
    Donante donante = new Donante(humana.getId());
    donacion = new Donacion(donante.getId());
    normalizador = new NormalizadorSemanticoBien(comparador);
  }

  @Test
  void normalizar_cuandoHayCoincidenciaTotal_debeRetornarAceptado() {
    Subcategoria subEscolares = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategorias.add(subEscolares);

    Bien bien = new Bien("una silla de madera para la escuela", "imagen.png", null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 5));

    List<ItemDonacionNormalizado> resultado =
        normalizador.normalizar(donacion, subcategorias, categoriasPorId, 1.0);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.getFirst();
    assertEquals(subEscolares.getId(), itemNormalizado.getBien().subcategoriaId());
    assertEquals(1.0, itemNormalizado.getBien().confianza());
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_cuandoCoincidenciaEsParcialBajoUmbral_debeRetornarPendienteRevision() {
    Subcategoria subEscolares = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategorias.add(subEscolares);

    Bien bien = new Bien("silla de plastico", "imagen.png", null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 3));

    List<ItemDonacionNormalizado> resultado =
        normalizador.normalizar(donacion, subcategorias, categoriasPorId, 1.0);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.getFirst();
    assertEquals(subEscolares.getId(), itemNormalizado.getBien().subcategoriaId());
    double expectedConfidence = 2.0 / 3.0;
    assertEquals(expectedConfidence, itemNormalizado.getBien().confianza(), 0.001);
    assertEquals(
        EstadoNormalizacion.PENDIENTE_REVISION, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_cuandoCoincidenciaEsParcialPeroSobreUmbralConfigurado_debeRetornarAceptado() {
    Subcategoria subEscolares = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategorias.add(subEscolares);

    Bien bien = new Bien("silla de plastico", "imagen.png", null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 3));

    List<ItemDonacionNormalizado> resultado =
        normalizador.normalizar(donacion, subcategorias, categoriasPorId, 0.5);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.getFirst();
    assertEquals(subEscolares.getId(), itemNormalizado.getBien().subcategoriaId());
    double expectedConfidence = 2.0 / 3.0;
    assertEquals(expectedConfidence, itemNormalizado.getBien().confianza(), 0.001);
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_cuandoNoHayCoincidencia_debeRetornarPendienteRevisionYFallback() {
    Subcategoria subEscolares = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subEscolares.agregarAlias("silla");
    Subcategoria subRopa = new Subcategoria(categoria.getId(), "Ropa");
    subRopa.agregarAlias("pantalon");

    subcategorias.add(subEscolares);
    subcategorias.add(subRopa);

    Bien bien = new Bien("manzana roja", "imagen.png", null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 2));

    List<ItemDonacionNormalizado> resultado =
        normalizador.normalizar(donacion, subcategorias, categoriasPorId, 0.5);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.getFirst();
    assertNotNull(itemNormalizado.getBien().subcategoriaId());
    assertEquals(0.0, itemNormalizado.getBien().confianza());
    assertEquals(
        EstadoNormalizacion.PENDIENTE_REVISION, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_conMultiplesSubcategorias_debeElegirLaDeMejorConfianza() {
    Subcategoria subOficina = new Subcategoria(categoria.getId(), "Muebles de Oficina");
    subOficina.agregarAlias("silla de madera");

    Subcategoria subMesaLuz = new Subcategoria(categoria.getId(), "Mesa de luz");
    subMesaLuz.agregarAlias("mesa de luz");

    Subcategoria subVelador = new Subcategoria(categoria.getId(), "Velador");
    subVelador.agregarAlias("luz");

    subcategorias.add(subOficina);
    subcategorias.add(subMesaLuz);
    subcategorias.add(subVelador);

    Bien bien = new Bien("silla de luz", "imagen.png", null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 1));

    List<ItemDonacionNormalizado> resultado =
        normalizador.normalizar(donacion, subcategorias, categoriasPorId, 1.0);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.getFirst();
    assertEquals(subVelador.getId(), itemNormalizado.getBien().subcategoriaId());
    assertEquals(1.0, itemNormalizado.getBien().confianza());
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_cuandoNoHaySubcategorias_debeLanzarExcepcion() {
    Bien bien = new Bien("silla", "imagen.png", null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 1));

    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> normalizador.normalizar(donacion, List.of(), categoriasPorId, 1.0));

    assertEquals(ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA, exception.getError());
  }
}
