package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.CategoriaMother;
import grupo5.donaciones.infrastructure.CatalogDataInitializer;
import grupo5.donaciones.infrastructure.persistencia.mapper.CategoriasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.SubcategoriasPersistenciaMapper;
import grupo5.donaciones.models.entities.categorias.AliasSubcategoria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// El seeder viene apagado por defecto (SEED_CATALOGO_ENABLED=false): acá se lo activa a propósito.
@DataJpaTest(properties = "donatrack.catalogo.seed-enabled=true")
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  CategoriasPersistenciaMapper.class,
  SubcategoriasPersistenciaMapper.class,
  CategoriaRepositoryJpaAdapter.class,
  SubcategoriaRepositoryJpaAdapter.class,
  CatalogDataInitializer.class
})
// Sin transacción de test: en producción los adapters no son transaccionales, cada operación del
// repo corre en su propia transacción y el mapeo a dominio ocurre afuera.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class CatalogoRepositoriesJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private ICategoriasRepository categorias;
  @Autowired private ISubcategoriasRepository subcategorias;
  @Autowired private CatalogDataInitializer seeder;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    subcategorias.deleteAll();
    categorias.deleteAll();
  }

  private static Set<String> textos(Subcategoria subcategoria) {
    return subcategoria.getAliases().stream()
        .map(AliasSubcategoria::alias)
        .collect(Collectors.toSet());
  }

  private long cantidadDeFilasDeAlias() {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones.alias", Long.class);
  }

  @Test
  void guardarYLeer_categoria_conservaSusCampos() {
    Categoria original = new Categoria("Alimentos", true, false, Unidad.KILOGRAMO);

    categorias.save(original);
    Categoria leida = categorias.findById(original.getId()).orElseThrow();

    assertEquals(original.getId(), leida.getId());
    assertEquals("Alimentos", leida.getNombre());
    assertTrue(leida.getConUso());
    assertFalse(leida.getConVencimiento());
    assertEquals(Unidad.KILOGRAMO, leida.getTipoUnidad());
  }

  @Test
  void guardar_categoriaActualizada_persisteLosCambios() {
    Categoria categoria = CategoriaMother.ropa();
    categorias.save(categoria);

    categoria.actualizar("Indumentaria", true, true, Unidad.METRO);
    categorias.save(categoria);
    Categoria leida = categorias.findById(categoria.getId()).orElseThrow();

    assertEquals("Indumentaria", leida.getNombre());
    assertEquals(Unidad.METRO, leida.getTipoUnidad());
    assertEquals(1, categorias.count());
  }

  @Test
  void guardarYLeer_subcategoria_conservaCategoriaNombreYAlias() {
    Categoria categoria = CategoriaMother.alimentos();
    categorias.save(categoria);
    Subcategoria original = CategoriaMother.arroz(categoria);

    subcategorias.save(original);
    Subcategoria leida = subcategorias.findById(original.getId()).orElseThrow();

    assertEquals(categoria.getId(), leida.getCategoriaId());
    assertEquals("Arroz", leida.getNombre());
    assertEquals(Set.of("arroz blanco", "arroz integral"), textos(leida));
    assertTrue(leida.getAliases().stream().allMatch(a -> "Arroz".equals(a.subcategoriaNombre())));
  }

  @Test
  void guardar_alQuitarUnAliasYAgregarOtro_dejaSoloLosVigentesSinFilasHuerfanas() {
    Categoria categoria = CategoriaMother.ropa();
    categorias.save(categoria);
    Subcategoria subcategoria = CategoriaMother.camperas(categoria);
    subcategorias.save(subcategoria);

    subcategoria.removerAlias("abrigo");
    subcategoria.agregarAlias("parka");
    subcategorias.save(subcategoria);
    Subcategoria leida = subcategorias.findById(subcategoria.getId()).orElseThrow();

    assertEquals(Set.of("campera de abrigo", "camperas", "parka"), textos(leida));
    assertEquals(3, cantidadDeFilasDeAlias());
  }

  @Test
  void guardar_subcategoriaRenombrada_losAliasLeidosTraenElNombreNuevo() {
    Categoria categoria = CategoriaMother.alimentos();
    categorias.save(categoria);
    Subcategoria subcategoria = CategoriaMother.arroz(categoria);
    subcategorias.save(subcategoria);

    subcategoria.actualizar(categoria.getId(), "Arroces");
    subcategorias.save(subcategoria);
    Subcategoria leida = subcategorias.findById(subcategoria.getId()).orElseThrow();

    assertTrue(leida.getAliases().stream().allMatch(a -> "Arroces".equals(a.subcategoriaNombre())));
  }

  @Test
  void eliminar_subcategoria_borraSusAlias() {
    Categoria categoria = CategoriaMother.alimentos();
    categorias.save(categoria);
    Subcategoria subcategoria = CategoriaMother.arroz(categoria);
    subcategorias.save(subcategoria);

    subcategorias.delete(subcategoria);

    assertEquals(0, subcategorias.count());
    assertEquals(0, cantidadDeFilasDeAlias());
  }

  @Test
  void guardar_subcategoriaConCategoriaInexistente_fallaPorClaveForanea() {
    Subcategoria huerfana = new Subcategoria(UUID.randomUUID(), "Sin categoría");

    assertThrows(DataIntegrityViolationException.class, () -> subcategorias.save(huerfana));
  }

  @Test
  void guardar_aliasRepetidoEnLaMismaSubcategoria_quedaUnaSolaFila() {
    Categoria categoria = CategoriaMother.alimentos();
    categorias.save(categoria);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Fideos");
    subcategoria.agregarAlias("tallarin");
    subcategoria.agregarAlias("tallarin");

    subcategorias.save(subcategoria);

    assertEquals(1, cantidadDeFilasDeAlias());
  }

  @Test
  void insertar_aliasRepetidoPorFueraDelDominio_fallaPorRestriccionUnica() {
    Categoria categoria = CategoriaMother.alimentos();
    categorias.save(categoria);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Fideos");
    subcategoria.agregarAlias("tallarin");
    subcategorias.save(subcategoria);

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO donaciones.alias (id, id_subcategoria, alias) VALUES (?, ?, ?)",
                UUID.randomUUID(),
                subcategoria.getId(),
                "tallarin"));
  }

  @Test
  void seeder_alEjecutarseDosVeces_noDuplicaElCatalogoYRespetaLasRestricciones() throws Exception {
    seeder.run();
    long categoriasTrasLaPrimera = categorias.count();
    long subcategoriasTrasLaPrimera = subcategorias.count();

    seeder.run();

    assertTrue(categoriasTrasLaPrimera > 0);
    assertEquals(categoriasTrasLaPrimera, categorias.count());
    assertEquals(subcategoriasTrasLaPrimera, subcategorias.count());
    List<Subcategoria> todas = subcategorias.findAll();
    assertTrue(todas.stream().noneMatch(s -> s.getAliases().isEmpty()));
  }
}
