package grupo5.donaciones.infrastructure.analizadores;

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
import grupo5.donaciones.models.repositories.impl.SubcategoriasRepositoryEnMemoria;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalizadorSemanticoBienTest {

  private ComparadorTexto comparador;
  private SubcategoriasRepositoryEnMemoria subcategoriaRepository;
  private Categoria categoria;
  private Donacion donacion;

  @BeforeEach
  void setUp() {
    comparador = new ComparadorTexto(new NormalizadorBasicoTexto());
    subcategoriaRepository = new SubcategoriasRepositoryEnMemoria();
    subcategoriaRepository.deleteAll();

    categoria = new Categoria("Varios", false, false, Unidad.UNIDADES);
    donacion = new Donacion(new Donante(new Humana("Juan", "Perez", LocalDate.now())));
  }

  @Test
  void normalizar_cuandoHayCoincidenciaTotal_debeRetornarAceptado() {
    Subcategoria subEscolares = new Subcategoria(categoria, "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategoriaRepository.save(subEscolares);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(comparador, subcategoriaRepository, 1.0);

    Bien bien = new Bien("una silla de madera para la escuela", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 5));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    assertEquals(subEscolares, itemNormalizado.getBien().getSubcategoria());
    assertEquals(1.0, itemNormalizado.getBien().getConfianza());
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().getEstadoNormalizacion());
  }

  @Test
  void normalizar_cuandoCoincidenciaEsParcialBajoUmbral_debeRetornarPendienteRevision() {
    Subcategoria subEscolares = new Subcategoria(categoria, "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategoriaRepository.save(subEscolares);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(comparador, subcategoriaRepository, 1.0);

    Bien bien = new Bien("silla de plastico", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 3));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    assertEquals(subEscolares, itemNormalizado.getBien().getSubcategoria());
    // "silla de plastico" y "silla de madera" tienen "silla" y "de" en común (2 palabras de 3 en el
    // alias)
    double expectedConfidence = 2.0 / 3.0;
    assertEquals(expectedConfidence, itemNormalizado.getBien().getConfianza(), 0.001);
    assertEquals(
        EstadoNormalizacion.PENDIENTE_REVISION, itemNormalizado.getBien().getEstadoNormalizacion());
  }

  @Test
  void normalizar_cuandoCoincidenciaEsParcialPeroSobreUmbralConfigurado_debeRetornarAceptado() {
    Subcategoria subEscolares = new Subcategoria(categoria, "Muebles Escolares");
    subEscolares.agregarAlias("silla de madera");
    subcategoriaRepository.save(subEscolares);

    // Umbral de aceptación configurado en 0.5 (50% de coincidencia)
    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(comparador, subcategoriaRepository, 0.5);

    Bien bien = new Bien("silla de plastico", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 3));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    assertEquals(subEscolares, itemNormalizado.getBien().getSubcategoria());
    double expectedConfidence = 2.0 / 3.0;
    assertEquals(expectedConfidence, itemNormalizado.getBien().getConfianza(), 0.001);
    // Como 0.666 > 0.5, debe ser ACEPTADO
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().getEstadoNormalizacion());
  }

  @Test
  void normalizar_cuandoNoHayCoincidencia_debeRetornarPendienteRevisionYFallback() {
    Subcategoria subEscolares = new Subcategoria(categoria, "Muebles Escolares");
    subEscolares.agregarAlias("silla");
    Subcategoria subRopa = new Subcategoria(categoria, "Ropa");
    subRopa.agregarAlias("pantalon");

    // Guardamos subEscolares primero, por lo que será el fallback
    subcategoriaRepository.save(subEscolares);
    subcategoriaRepository.save(subRopa);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(comparador, subcategoriaRepository, 0.5);

    Bien bien = new Bien("manzana roja", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 2));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    // El fallback puede ser cualquiera de las dos subcategorias (ConcurrentHashMap no garantiza
    // orden)
    assertNotNull(itemNormalizado.getBien().getSubcategoria());
    assertEquals(0.0, itemNormalizado.getBien().getConfianza());
    assertEquals(
        EstadoNormalizacion.PENDIENTE_REVISION, itemNormalizado.getBien().getEstadoNormalizacion());
  }

  @Test
  void normalizar_conMultiplesSubcategorias_debeElegirLaDeMejorConfianza() {
    Subcategoria subOficina = new Subcategoria(categoria, "Muebles de Oficina");
    subOficina.agregarAlias(
        "silla de madera"); // 2/3 en comun con "silla de luz" (silla, de) -> confianza 0.666

    Subcategoria subMesaLuz = new Subcategoria(categoria, "Mesa de luz");
    subMesaLuz.agregarAlias(
        "mesa de luz"); // 2/3 en comun con "silla de luz" (de, luz) -> confianza 0.666

    Subcategoria subVelador = new Subcategoria(categoria, "Velador");
    subVelador.agregarAlias("luz"); // 1/1 en comun con "silla de luz" (luz) -> confianza 1.0

    subcategoriaRepository.save(subOficina);
    subcategoriaRepository.save(subMesaLuz);
    subcategoriaRepository.save(subVelador);

    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(comparador, subcategoriaRepository, 1.0);

    Bien bien = new Bien("silla de luz", "imagen.png", null, null);
    donacion.agregarItem(new ItemDonacion(bien, 1));

    List<ItemDonacionNormalizado> resultado = normalizador.normalizar(donacion);

    assertEquals(1, resultado.size());
    ItemDonacionNormalizado itemNormalizado = resultado.get(0);
    assertEquals(subVelador, itemNormalizado.getBien().getSubcategoria());
    assertEquals(1.0, itemNormalizado.getBien().getConfianza());
    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().getEstadoNormalizacion());
  }

  @Test
  void normalizar_cuandoNoHaySubcategorias_debeLanzarExcepcion() {
    NormalizadorSemanticoBien normalizador =
        new NormalizadorSemanticoBien(comparador, subcategoriaRepository, 1.0);

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
