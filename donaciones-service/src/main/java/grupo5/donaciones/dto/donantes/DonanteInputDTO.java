package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.dto.personas.PersonaInputDTO;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DonanteInputDTO {
  private PersonaInputDTO persona;
  private String canalContacto;
}
