package grupo5.donaciones.models.entities.personas.direccion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Localidad {
  private String nombre;
  private Provincia provincia;
}
