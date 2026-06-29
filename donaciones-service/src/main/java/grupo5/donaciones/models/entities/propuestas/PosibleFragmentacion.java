package grupo5.donaciones.models.entities.propuestas;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosibleFragmentacion {
  Long id;
  DonacionIndependiente donacionOriginal;
  Integer cantidadNecesaria;
}
