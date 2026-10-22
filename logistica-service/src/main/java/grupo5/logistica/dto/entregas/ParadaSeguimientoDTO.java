package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.LocalDateTime;

/**
 * Vista sanitizada de una parada dentro de una ruta para seguimiento de donantes (RF-09) y
 * beneficiarios (RF-14) sin GPS en tiempo real. No expone direcciones, donaciones ni beneficiarios
 * de otras entregas.
 */
public record ParadaSeguimientoDTO(
    int ordenVisita,
    EstadoEntrega estado,
    boolean esEntregaConsultada,
    LocalDateTime horaArribo) {}
