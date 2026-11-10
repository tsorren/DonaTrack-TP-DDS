package grupo5.logistica.models.entities.entregas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

/**
 * Problema que la entidad cuenta al confirmar una entrega que llegó incompleta, dañada o con otra
 * cosa rara. No cambia el estado: la entrega queda {@code ENTREGADA} (ADR del contrato de entregas,
 * decisión I1).
 *
 * <p>Que la cantidad recibida sea menor que la enviada lo valida el cliente, que tiene la donación:
 * Logística no guarda la cantidad (ADR, decisión P1).
 *
 * @param motivo obligatorio
 * @param detalle obligatorio con {@link MotivoReporte#OTRO}; hasta 500 caracteres
 * @param cantidadRecibida en la unidad de la donación, mayor que cero; obligatoria con {@link
 *     MotivoReporte#INCOMPLETA}
 */
public record ReporteRecepcion(MotivoReporte motivo, String detalle, Double cantidadRecibida) {

  static final int MAX_DETALLE = 500;

  public ReporteRecepcion {
    if (motivo == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    detalle = detalle == null || detalle.isBlank() ? null : detalle.trim();
    if (detalle != null && detalle.length() > MAX_DETALLE) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (motivo == MotivoReporte.OTRO && detalle == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (cantidadRecibida != null && (cantidadRecibida <= 0 || !Double.isFinite(cantidadRecibida))) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (motivo == MotivoReporte.INCOMPLETA && cantidadRecibida == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
