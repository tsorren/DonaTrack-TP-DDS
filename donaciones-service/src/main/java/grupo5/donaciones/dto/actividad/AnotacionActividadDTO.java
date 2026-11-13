package grupo5.donaciones.dto.actividad;

/**
 * El cuerpo de {@code POST /api/actividad} (R11-26): una acción que pasó fuera de Donaciones
 * (Keycloak o Logística). El autor y la hora los pone Donaciones; el cuerpo no los trae.
 */
public record AnotacionActividadDTO(String accion, String objetoId, String detalle) {}
