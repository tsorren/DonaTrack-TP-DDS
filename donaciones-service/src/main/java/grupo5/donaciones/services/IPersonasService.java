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

  void eliminarPersona(UUID id);

  UUID obtenerIdPersonaAdministradora();
}
