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

  private BienNormalizado normalizarBien(
      ItemDonacion item,
      List<Subcategoria> subcategorias,
      Map<UUID, Categoria> categoriasPorId,
      double umbralAceptacion) {
    validarEntrada(item, subcategorias);

    Bien bien = item.bien();
    Coincidencia mejor = buscarMejorCoincidencia(bien.descripcion(), subcategorias);

    Categoria categoria =
        (mejor.subcategoria().getCategoriaId() != null && categoriasPorId != null)
            ? categoriasPorId.get(mejor.subcategoria().getCategoriaId())
            : null;

    boolean conVencimiento =
        categoria != null && Boolean.TRUE.equals(categoria.getConVencimiento());
    boolean conUso = categoria != null && Boolean.TRUE.equals(categoria.getConUso());

    boolean cumpleRestricciones =
        (!conVencimiento || bien.fechaVencimiento() != null)
            && (conVencimiento || bien.fechaVencimiento() == null)
            && (!conUso || bien.estado() != null);

    EstadoNormalizacion estado =
        (mejor.confianza() >= umbralAceptacion && cumpleRestricciones)
            ? EstadoNormalizacion.ACEPTADO
            : EstadoNormalizacion.PENDIENTE_REVISION;

    return new BienNormalizado(
        bien, mejor.subcategoria().getId(), mejor.confianza(), estado, conVencimiento, conUso);
  }

  private static void validarEntrada(ItemDonacion item, List<Subcategoria> subcategorias) {
    if (item == null || item.bien() == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_BIEN);
    }
    if (subcategorias == null || subcategorias.isEmpty()) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA);
    }
  }

  private Coincidencia buscarMejorCoincidencia(
      String descripcion, List<Subcategoria> subcategorias) {
    Subcategoria subcategoriaElegida = subcategorias.getFirst();
    double mejorConfianza = 0.0;

    for (Subcategoria subcategoria : subcategorias) {
      for (AliasSubcategoria aliasObj : subcategoria.getAliases()) {
        double confianzaActual = calcularConfianza(descripcion, aliasObj.alias());
        if (confianzaActual > mejorConfianza) {
          mejorConfianza = confianzaActual;
          subcategoriaElegida = subcategoria;
        }
      }
    }
    return new Coincidencia(subcategoriaElegida, mejorConfianza);
  }

  private double calcularConfianza(String descripcion, String alias) {
    int palabrasEnComun = comparador.contarPalabrasEnComun(descripcion, alias);
    int palabrasAlias = comparador.contarPalabrasEnComun(alias, alias);
    return palabrasAlias > 0 ? (double) palabrasEnComun / palabrasAlias : 0.0;
  }

  private record Coincidencia(Subcategoria subcategoria, double confianza) {}
}
