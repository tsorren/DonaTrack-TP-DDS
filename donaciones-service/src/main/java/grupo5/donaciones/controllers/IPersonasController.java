package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface IPersonasController {

  ResponseEntity<PersonaOutputDTO> crearPersona(PersonaInputDTO persona);

  ResponseEntity<List<PersonaOutputDTO>> consultarPersonas(
      @RequestParam(required = false) TipoPersona tipo);
}
