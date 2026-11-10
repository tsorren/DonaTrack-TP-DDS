package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comando {@code entrega.cancelada.<proveedorId>.v1} del broker: le pide al proveedor que cancele
 * la entrega de una donación que todavía no salió (HB-19).
 */
public record EventoEntregaCanceladaV1(
    UUID donacionIndependienteId,
    String motivo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fecha) {}
