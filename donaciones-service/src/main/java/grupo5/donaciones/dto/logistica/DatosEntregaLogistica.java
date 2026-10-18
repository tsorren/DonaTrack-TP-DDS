package grupo5.donaciones.dto.logistica;

import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo canónico del pedido de entrega que Donaciones le hace a cualquier proveedor de logística.
 * Cada adapter lo traduce al contrato de su proveedor (AMQP o HTTP).
 */
public record DatosEntregaLogistica(
    UUID donacionIndependienteId,
    UUID personaBeneficiariaId,
    DestinoEventoDTO destino,
    Double pesoTotalKG,
    Double volumenTotalM3,
    LocalDateTime fecha) {}
