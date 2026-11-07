package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Datos guardados de una {@link EntradaOutboxLogistica}, para reconstituirla al leerla de la base
 * (mismo criterio que {@code DatosDonacion}: evita un constructor con demasiados parámetros).
 */
public record RegistroEntradaOutbox(
    UUID id,
    UUID solicitudId,
    String proveedorId,
    DatosEntregaLogistica datos,
    String traceId,
    int maxIntentos,
    int intentos,
    LocalDateTime proximoIntento,
    EstadoEntradaOutbox estado) {}
