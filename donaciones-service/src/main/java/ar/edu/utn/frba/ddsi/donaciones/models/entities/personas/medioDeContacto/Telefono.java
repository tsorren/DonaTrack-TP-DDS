package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Telefono extends MedioDeContacto {
  private String caracteristica;
  private String codigoArea;
  private String numero;
}
