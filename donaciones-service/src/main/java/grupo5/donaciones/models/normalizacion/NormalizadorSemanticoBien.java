package grupo5.donaciones.models.normalizacion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.AliasSubcategoria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NormalizadorSemanticoBien {

  private final ComparadorTexto comparador;

  public NormalizadorSemanticoBien(ComparadorTexto comparador) {
    this.comparador =
        comparador != null ? comparador : new ComparadorTexto(new NormalizadorBasicoTexto());
  }

  public NormalizadorSemanticoBien() {
    this(new ComparadorTexto(new NormalizadorBasicoTexto()));
  }

  public List<ItemDonacionNormalizado> normalizar(
      Donacion donacion,
      List<Subcategoria> subcategorias,
      Map<UUID, Categoria> categoriasPorId,
      double umbralAceptacion) {
    if (donacion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    List<ItemDonacionNormalizado> itemsNormalizados = new ArrayList<>();
    for (ItemDonacion item : donacion.getItems()) {
      BienNormalizado bienNormalizado =
          normalizarBien(item, subcategorias, categoriasPorId, umbralAceptacion);
      itemsNormalizados.add(
          new ItemDonacionNormalizado(donacion.getId(), bienNormalizado, item.cantidad()));
    }
    return itemsNormalizados;
  }

  public BienNormalizado normalizarBien(
      ItemDonacion item,
      List<Subcategoria> subcategorias,
      Map<UUID, Categoria> categoriasPorId,
      double umbralAceptacion) {
    if (item == null || item.bien() == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_BIEN);
    }
    if (subcategorias == null || subcategorias.isEmpty()) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA);
    }

    Bien bien = item.bien();
    String descripcion = bien.descripcion();

    Subcategoria subcategoriaElegida = subcategorias.getFirst();
    double mejorConfianza = 0.0;

    for (Subcategoria subcategoria : subcategorias) {
      for (AliasSubcategoria aliasObj : subcategoria.getAliases()) {
        String alias = aliasObj.alias();
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

    Categoria categoria =
        (subcategoriaElegida.getCategoriaId() != null && categoriasPorId != null)
            ? categoriasPorId.get(subcategoriaElegida.getCategoriaId())
            : null;

    boolean conVencimiento =
        categoria != null && Boolean.TRUE.equals(categoria.getConVencimiento());
    boolean conUso = categoria != null && Boolean.TRUE.equals(categoria.getConUso());

    return new BienNormalizado(
        bien, subcategoriaElegida.getId(), mejorConfianza, estado, conVencimiento, conUso);
  }
}
