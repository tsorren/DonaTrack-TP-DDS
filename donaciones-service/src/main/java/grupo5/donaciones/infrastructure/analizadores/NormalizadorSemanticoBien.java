package grupo5.donaciones.infrastructure.analizadores;

import grupo5.donaciones.models.entities.categorias.AliasSubcategoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NormalizadorSemanticoBien {

  private final ComparadorTexto comparador;
  private final ISubcategoriasRepository subcategoriaRepository;
  private final double umbralAceptacion;

  public NormalizadorSemanticoBien(
      ComparadorTexto comparador,
      ISubcategoriasRepository subcategoriaRepository,
      @Value("${donatrack.normalizacion.umbral-aceptacion:0.6}") double umbralAceptacion) {
    this.comparador = comparador;
    this.subcategoriaRepository = subcategoriaRepository;
    this.umbralAceptacion = umbralAceptacion;
  }

  public List<ItemDonacionNormalizado> normalizar(Donacion donacion) {
    List<Subcategoria> subcategorias = subcategoriaRepository.findAll();
    List<ItemDonacionNormalizado> itemsNormalizados = new ArrayList<>();
    donacion
        .getItems()
        .forEach(
            i ->
                itemsNormalizados.add(
                    new ItemDonacionNormalizado(
                        donacion, normalizarBien(i, subcategorias), i.getCantidad())));

    return itemsNormalizados;
  }

  private BienNormalizado normalizarBien(ItemDonacion item, List<Subcategoria> subcategorias) {
    Bien bien = item.getBien();
    String descripcion = bien.getDescripcion();

    if (subcategorias == null || subcategorias.isEmpty()) {
      throw new grupo5.common.exceptions.ValidationException(
          grupo5.common.exceptions.ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA);
    }

    // Default/fallback initialization: first subcategory with 0.0 confidence
    Subcategoria subcategoriaElegida = subcategorias.getFirst();
    double mejorConfianza = 0.0;

    for (Subcategoria subcategoria : subcategorias) {
      for (AliasSubcategoria aliasObj : subcategoria.getAliases()) {
        String alias = aliasObj.getAlias();
        int palabrasEnComun = comparador.contarPalabrasEnComun(descripcion, alias);
        int palabrasAlias = comparador.contarPalabrasEnComun(alias, alias);

        double confianzaActual = palabrasAlias > 0 ? (double) palabrasEnComun / palabrasAlias : 0.0;

        if (confianzaActual > mejorConfianza) {
          mejorConfianza = confianzaActual;
          subcategoriaElegida = subcategoria;
        }
      }
    }

    EstadoNormalizacion estado =
        mejorConfianza >= umbralAceptacion
            ? EstadoNormalizacion.ACEPTADO
            : EstadoNormalizacion.PENDIENTE_REVISION;

    return new BienNormalizado(bien, subcategoriaElegida, mejorConfianza, estado);
  }
}
