package grupo5.donaciones.models.entities.itemsNormalizados;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienNormalizado {
  private Bien bienOriginal;
  private Subcategoria subcategoria;
  private Double confianza;
  private EstadoNormalizacion estadoNormalizacion;

  public BienNormalizado(
      Bien bienOriginal,
      Subcategoria subcategoria,
      Double confianza,
      EstadoNormalizacion estadoNormalizacion) {
    validarReglasDeNegocio(bienOriginal, subcategoria, confianza, estadoNormalizacion);

    this.bienOriginal = bienOriginal;
    this.subcategoria = subcategoria;
    this.confianza = confianza;
    this.estadoNormalizacion = estadoNormalizacion;
  }

  private void validarReglasDeNegocio(
      Bien bienOriginal,
      Subcategoria subcategoria,
      Double confianza,
      EstadoNormalizacion estadoNormalizacion) {
    if (bienOriginal == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_BIEN);
    }
    if (subcategoria == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA);
    }
    if (confianza < 0 || confianza > 1.0) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_RANGO_CONFIANZA);
    }

    assert bienOriginal != null;
    assert subcategoria != null;
    Categoria categoria = subcategoria.getCategoria();
    if (categoria != null) {
      // 3. Validar bienes cuya categoría requiera vencimiento
      if (Boolean.TRUE.equals(categoria.getConVencimiento())
          && bienOriginal.getFechaVencimiento() == null) {
        throw new ValidationException(ErrorCatalog.BIEN_VENCIMIENTO_REQUERIDO);
      }

      // 4. Validar bienes cuya categoría no requiera vencimiento
      if (Boolean.FALSE.equals(categoria.getConVencimiento())
          && bienOriginal.getFechaVencimiento() != null) {
        throw new ValidationException(ErrorCatalog.BIEN_VENCIMIENTO_NO_PERMITIDO);
      }

      // 5. Validar bienes cuya categoría requiera uso (estado)
      if (Boolean.TRUE.equals(categoria.getConUso()) && bienOriginal.getEstado() == null) {
        throw new ValidationException(ErrorCatalog.BIEN_ESTADO_REQUERIDO);
      }
    }
  }

  public Boolean estaVencido() {
    return bienOriginal.estaVencido();
  }
}
