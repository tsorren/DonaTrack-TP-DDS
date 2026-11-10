package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.MotivoReporte;
import grupo5.logistica.models.entities.entregas.ReporteRecepcion;

/**
 * Reporte de una entrega que llegó con un problema: en la confirmación, en {@code
 * EntregaResponseDTO} y en {@code entrega.exitosa} (ADR del contrato de entregas, DTI-29).
 */
public record ReporteRecepcionDTO(MotivoReporte motivo, String detalle, Double cantidadRecibida) {

  public static ReporteRecepcionDTO de(ReporteRecepcion reporte) {
    return reporte == null
        ? null
        : new ReporteRecepcionDTO(reporte.motivo(), reporte.detalle(), reporte.cantidadRecibida());
  }

  public ReporteRecepcion aDominio() {
    return new ReporteRecepcion(motivo, detalle, cantidadRecibida);
  }
}
