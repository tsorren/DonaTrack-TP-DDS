package grupo5.donaciones.models.entities.itemsNormalizados;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.Bien;
import java.util.UUID;

public record BienNormalizado(
    Bien bienOriginal,
    UUID subcategoriaId,
    Double confianza,
    EstadoNormalizacion estadoNormalizacion) {

  public BienNormalizado(
      Bien bienOriginal,
      UUID subcategoriaId,
      Double confianza,
      EstadoNormalizacion estadoNormalizacion,
      boolean conVencimiento,
      boolean conEstado) {
    this(bienOriginal, subcategoriaId, confianza, estadoNormalizacion);
    validar(
        bienOriginal, subcategoriaId, confianza, estadoNormalizacion, conVencimiento, conEstado);
  }

  private static void validar(
      Bien bienOriginal,
      UUID subcategoriaId,
      Double confianza,
      EstadoNormalizacion estadoNormalizacion,
      boolean conVencimiento,
      boolean conEstado) {
    if (bienOriginal == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_BIEN);
    }
    if (subcategoriaId == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_SUBCATEGORIA);
    }
    if (confianza == null || confianza < 0 || confianza > 1.0) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_RANGO_CONFIANZA);
    }
    if (estadoNormalizacion == null) {
      throw new ValidationException(ErrorCatalog.BIEN_NORMALIZADO_SIN_ESTADO);
    }

    if (conVencimiento && bienOriginal.fechaVencimiento() == null) {
      throw new ValidationException(ErrorCatalog.BIEN_VENCIMIENTO_REQUERIDO);
    }
    if (!conVencimiento && bienOriginal.fechaVencimiento() != null) {
      throw new ValidationException(ErrorCatalog.BIEN_VENCIMIENTO_NO_PERMITIDO);
    }
    if (conEstado && bienOriginal.estado() == null) {
      throw new ValidationException(ErrorCatalog.BIEN_ESTADO_REQUERIDO);
    }
  }

  public Boolean estaVencido() {
    return bienOriginal != null && bienOriginal.estaVencido();
  }
}
