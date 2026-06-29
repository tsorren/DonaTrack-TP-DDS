package grupo5.donaciones.infrastructure.analizadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.impl.SubcategoriasRepositoryEnMemoria;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalizadorSemanticoBienTest {

  private ComparadorTexto comparador;
  private SubcategoriasRepositoryEnMemoria subcategoriaRepository;
  private ICategoriasRepository categoriasRepository;
  private Categoria categoria;
  private Donacion donacion;

  @BeforeEach
  void setUp() {
    comparador = new ComparadorTexto(new NormalizadorBasicoTexto());
    subcategoriaRepository = new SubcategoriasRepositoryEnMemoria();
    subcategoriaRepository.deleteAll();
    categoriasRepository = mock(ICategoriasRepository.class);

    categoria = new Categoria("Varios", false, false, Unidad.UNIDADES);
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(2026, Month.JUNE, 18));
    Donante donante = new Donante(humana.getId());
    donacion = new Donacion(donante.getId());
  }

  @Test
  void normalizar_cuandoHayCoincidenciaTotal_debeRetornarAceptado() {
    Subcategoria subEscolares = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategoriaRepository.save(subEscolares);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(
            comparador, subcategoriaRepository, categoriasRepository, 1.0);

    Bien bien = new Bien("una silla de madera para la escuela", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 5));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    assertEquals(subEscolares.getId(), itemNormalizado.getBien().subcategoriaId());
    assertEquals(1.0, itemNormalizado.getBien().confianza());
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_cuandoCoincidenciaEsParcialBajoUmbral_debeRetornarPendienteRevision() {
    Subcategoria subEscolares = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategoriaRepository.save(subEscolares);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(
            comparador, subcategoriaRepository, categoriasRepository, 1.0);

    Bien bien = new Bien("silla de plastico", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 3));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
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
    subcategoriaRepository.save(subEscolares);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(
            comparador, subcategoriaRepository, categoriasRepository, 0.5);

    Bien bien = new Bien("silla de plastico", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 3));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
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

    subcategoriaRepository.save(subEscolares);
    subcategoriaRepository.save(subRopa);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(
            comparador, subcategoriaRepository, categoriasRepository, 0.5);

    Bien bien = new Bien("manzana roja", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 2));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
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

    subcategoriaRepository.save(subOficina);
    subcategoriaRepository.save(subMesaLuz);
    subcategoriaRepository.save(subVelador);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(
            comparador, subcategoriaRepository, categoriasRepository, 1.0);

    Bien bien = new Bien("silla de luz", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 1));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    assertEquals(subVelador.getId(), itemNormalizado.getBien().subcategoriaId());
    assertEquals(1.0, itemNormalizado.getBien().confianza());
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().estadoNormalizacion());
  }

  @Test
  void normalizar_cuandoNoHaySubcategorias_debeLanzarExcepcion() {
    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(
            comparador, subcategoriaRepository, categoriasRepository, 1.0);

    Bien bien = new Bien("silla", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 1));

    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              normalizador.normalizar(donacion);
            });

    assertEquals(ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA, exception.getError());
  }
}
