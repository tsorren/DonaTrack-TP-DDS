package grupo5.donaciones.dto.logistica;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Aviso que un proveedor de logística HTTP le manda a Donaciones. Según el {@code tipo} se usan
 * unos campos u otros; el resto se ignora:
 *
 * <ul>
 *   <li>{@code RUTA_ASIGNADA}: {@code rutaId}, {@code donacionIndependienteId}.
 *   <li>{@code RUTA_INICIADA}: {@code rutaId}, {@code donacionesIndependientesIds}, {@code urlMapa}
 *       (opcional).
 *   <li>{@code ENTREGA_EXITOSA}: {@code entregaId}, {@code donacionIndependienteId}, {@code
 *       patenteCamion} (opcional).
 *   <li>{@code ENTREGA_FALLIDA}: {@code entregaId}, {@code donacionIndependienteId}, {@code
 *       justificacion}, {@code replanificable}.
 * </ul>
 *
 * El identificador de ruta o de entrega es la clave de idempotencia: reenviar el mismo aviso no
 * cambia dos veces el estado de la donación.
 */
public record AvisoProveedorRequestDTO(
    @NotNull(message = "El tipo de aviso es obligatorio") TipoAvisoProveedor tipo,
    UUID rutaId,
    UUID entregaId,
    UUID donacionIndependienteId,
    List<UUID> donacionesIndependientesIds,
    String patenteCamion,
    String urlMapa,
    String justificacion,
    Boolean replanificable) {}
