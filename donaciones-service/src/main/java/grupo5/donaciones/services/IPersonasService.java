package grupo5.donaciones.services;

import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;

public interface IPersonasService {
  PersonaOutputDTO crearPersona(PersonaInputDTO input);

  List<PersonaOutputDTO> consultarPersonas(TipoPersona tipo);

  PersonaOutputDTO actualizarPersona(UUID id, PersonaInputDTO input);

  /**
   * Actualiza solo los datos informados en {@code input}: lo que viene nulo o vacío se conserva, y
   * los medios de contacto se agregan sin borrar los existentes. El tipo jurídico nunca cambia.
   */
  PersonaOutputDTO actualizarParcial(UUID id, PersonaInputDTO input);

  void eliminarPersona(UUID id);

  UUID obtenerIdPersonaAdministradora();
}
