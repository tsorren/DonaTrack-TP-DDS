package grupo5.donaciones.models.entities.propuestas;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosibleFragmentacion {
  Long id;
  UUID donacionOriginalId;
  Integer cantidadNecesaria;
}
