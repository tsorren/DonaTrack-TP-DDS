package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DonanteOutputDTO {
  private PersonaOutputDTO persona;
  private String canalContacto;
  UUID id;
}
