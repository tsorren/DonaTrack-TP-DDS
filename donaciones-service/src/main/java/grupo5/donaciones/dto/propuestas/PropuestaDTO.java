package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Una propuesta de asignación. Desde R11-24 (aditivo): {@code sugeridaPor} (qué algoritmos la
 * sugirieron y en qué puesto; vacío en las anteriores a ese cambio), {@code situacion} (solo en las
 * pendientes) y {@code otrasPendientesMismaNecesidad}.
 */
public record PropuestaDTO(
    UUID id,
    EstadoPropuesta estado,
    LocalDateTime fechaCreacion,
    NecesidadResumenDTO necesidad,
    List<GrupoFragmentacionDTO> fragmentaciones,
    String decididaPor,
    LocalDateTime fechaDecision,
    List<SugerenciaDTO> sugeridaPor,
    SituacionPropuesta situacion,
    int otrasPendientesMismaNecesidad) {

  public PropuestaDTO {
    sugeridaPor = sugeridaPor == null ? List.of() : List.copyOf(sugeridaPor);
  }

  /** Sin los datos de R11-24 (los tests de antes de ese cambio). */
  public PropuestaDTO(
      UUID id,
      EstadoPropuesta estado,
      LocalDateTime fechaCreacion,
      NecesidadResumenDTO necesidad,
      List<GrupoFragmentacionDTO> fragmentaciones,
      String decididaPor,
      LocalDateTime fechaDecision) {
    this(
        id,
        estado,
        fechaCreacion,
        necesidad,
        fragmentaciones,
        decididaPor,
        fechaDecision,
        List.of(),
        null,
        0);
  }
}
