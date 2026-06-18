package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface IPersonasController {

  ResponseEntity<PersonaOutputDTO> crearPersona(PersonaInputDTO persona);

  ResponseEntity<List<PersonaOutputDTO>> consultarPersonas(
      @RequestParam(required = false) TipoPersona tipo);

  ResponseEntity<PersonaOutputDTO> actualizarPersona(UUID id, PersonaInputDTO persona);

  ResponseEntity<Void> eliminarPersona(UUID id);
}
