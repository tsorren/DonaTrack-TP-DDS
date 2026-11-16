package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.personas.FiltroPersonas;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.services.ParametrosListado;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IPersonasController {

  ResponseEntity<PersonaOutputDTO> crearPersona(PersonaInputDTO persona);

  ResponseEntity<List<PersonaOutputDTO>> consultarPersonas(
      FiltroPersonas filtro, ParametrosListado listado);

  ResponseEntity<PersonaOutputDTO> actualizarPersona(UUID id, PersonaInputDTO persona);

  ResponseEntity<Void> eliminarPersona(UUID id);
}
