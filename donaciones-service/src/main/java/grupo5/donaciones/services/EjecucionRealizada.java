package grupo5.donaciones.services;

import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import java.util.List;
import java.util.UUID;

/**
 * Lo que deja una corrida de la asignación: el id de la ejecución guardada en el historial y las
 * propuestas nuevas. El controller responde solo las propuestas; el id va al registro de actividad
 * como {@code objetoId} de {@code ASIGNACION_CORRIDA} (R11-26).
 */
public record EjecucionRealizada(UUID ejecucionId, List<PropuestaDTO> propuestas) {}
