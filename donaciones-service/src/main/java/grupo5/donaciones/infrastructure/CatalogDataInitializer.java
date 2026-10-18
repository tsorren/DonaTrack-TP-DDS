package grupo5.donaciones.infrastructure;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "donatrack.catalogo.seed-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CatalogDataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(CatalogDataInitializer.class);
  private final ISubcategoriasRepository subcategoryRepository;
  private final ICategoriasRepository categoryRepository;

  public CatalogDataInitializer(
      ISubcategoriasRepository subcategoryRepository, ICategoriasRepository categoryRepository) {
    this.subcategoryRepository = subcategoryRepository;
    this.categoryRepository = categoryRepository;
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("Initializing catalog data (Categories and Subcategories) in memory...");

    // Categoria Alimentos
    Categoria alimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    categoryRepository.save(alimentos);

    Subcategoria noPerecederos = new Subcategoria(alimentos.getId(), "No Perecederos");
    noPerecederos.agregarAlias("arroz");
    noPerecederos.agregarAlias("fideos");
    noPerecederos.agregarAlias("alimentos");
    noPerecederos.agregarAlias("lentejas");
    noPerecederos.agregarAlias("porotos");
    noPerecederos.agregarAlias("comida");
    noPerecederos.agregarAlias("alimentos no perecederos");
    subcategoryRepository.save(noPerecederos);

    Subcategoria frutas = new Subcategoria(alimentos.getId(), "Frutas");
    frutas.agregarAlias("manzana");
    frutas.agregarAlias("banana");
    frutas.agregarAlias("fruta");
    subcategoryRepository.save(frutas);

    // Categoria Ropa
    Categoria ropa = new Categoria("Ropa", true, false, Unidad.UNIDADES);
    categoryRepository.save(ropa);

    Subcategoria ropaInvierno = new Subcategoria(ropa.getId(), "Ropa de Invierno");
    ropaInvierno.agregarAlias("pantalon");
    ropaInvierno.agregarAlias("abrigo");
    ropaInvierno.agregarAlias("campera");
    ropaInvierno.agregarAlias("buzo");
    ropaInvierno.agregarAlias("ropa");
    subcategoryRepository.save(ropaInvierno);

    Subcategoria ropaVerano = new Subcategoria(ropa.getId(), "Ropa de Verano");
    ropaVerano.agregarAlias("remera");
    ropaVerano.agregarAlias("short");
    ropaVerano.agregarAlias("malla");
    subcategoryRepository.save(ropaVerano);

    // Categoria Muebles
    Categoria muebles = new Categoria("Muebles", true, false, Unidad.UNIDADES);
    categoryRepository.save(muebles);

    Subcategoria mueblesEscolares = new Subcategoria(muebles.getId(), "Muebles Escolares");
    mueblesEscolares.agregarAlias("banco");
    mueblesEscolares.agregarAlias("pizarron");
    mueblesEscolares.agregarAlias("silla escolar");
    subcategoryRepository.save(mueblesEscolares);

    Subcategoria mueblesOficina = new Subcategoria(muebles.getId(), "Muebles de Oficina");
    mueblesOficina.agregarAlias("escritorio");
    mueblesOficina.agregarAlias("silla de oficina");
    subcategoryRepository.save(mueblesOficina);

    Subcategoria mesaDeLuz = new Subcategoria(muebles.getId(), "Mesa de luz");
    mesaDeLuz.agregarAlias("mesa de luz");
    subcategoryRepository.save(mesaDeLuz);

    Subcategoria velador = new Subcategoria(muebles.getId(), "Velador");
    velador.agregarAlias("velador");
    subcategoryRepository.save(velador);

    log.info(
        "Catalog data initialized successfully with {} subcategories.",
        subcategoryRepository.count());
  }
}
